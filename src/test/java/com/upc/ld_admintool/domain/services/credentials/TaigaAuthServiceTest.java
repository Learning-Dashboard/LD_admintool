package com.upc.ld_admintool.domain.services.credentials;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaigaAuthService")
class TaigaAuthServiceTest {

    private static final String API_URL = "https://taiga.example/api/v1";
    private static final String AUTH_URL = "https://taiga.example/api/v1/auth";
    private static final String OTHER_AUTH_URL = "https://other-taiga.example/api/v1/auth";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private RestTemplate restTemplate;

    private TaigaAuthService service;

    @BeforeEach
    void setUp() {
        service = new TaigaAuthService();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        setClock(NOW);
    }

    private void setClock(Instant instant) {
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static JsonNode loginResponse(String token) {
        return new ObjectMapper().createObjectNode().put("auth_token", token);
    }

    private static TaigaContext login(String authUrl, String username, String password) {
        return TaigaContext.withLogin(API_URL, authUrl, username, password);
    }

    // ---- Static token ---------------------------------------------------------

    @Test
    @DisplayName("an application token (with colons) uses the Application scheme, without login")
    void applicationToken() {
        TaigaContext context = TaigaContext.withToken(API_URL, AUTH_URL, "payload:timestamp:signature");

        assertEquals("Application payload:timestamp:signature", service.authorizationValue(context));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("a JWT (without colons) uses the Bearer scheme")
    void bearerToken() {
        TaigaContext context = TaigaContext.withToken(API_URL, AUTH_URL, "eyJhbGciOiJIUzI1NiJ9.payload.signature");

        assertEquals("Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature", service.authorizationValue(context));
    }

    @Test
    @DisplayName("a scheme saved with the token is not sent twice")
    void stripsSavedScheme() {
        assertEquals("Application a:b:c", TaigaAuthService.staticTokenAuthorization("Application a:b:c"));
        assertEquals("Application a:b:c", TaigaAuthService.staticTokenAuthorization("  Application   a:b:c "));
        assertEquals("Bearer jwt", TaigaAuthService.staticTokenAuthorization("Bearer jwt"));
    }

    @Test
    @DisplayName("an empty token is rejected")
    void emptyTokenRejected() {
        assertThrows(IllegalArgumentException.class, () -> TaigaAuthService.staticTokenAuthorization("  "));
        assertThrows(IllegalArgumentException.class, () -> TaigaAuthService.staticTokenAuthorization("Bearer "));
        assertThrows(IllegalArgumentException.class, () -> TaigaAuthService.staticTokenAuthorization(null));
    }

    // ---- Login ----------------------------------------------------------------

    @Test
    @DisplayName("without a token it logs in at the auth URL of the context")
    void logsInAtContextAuthUrl() {
        when(restTemplate.postForObject(eq(AUTH_URL), any(), eq(JsonNode.class)))
                .thenReturn(loginResponse("login:jwt"));

        // A login token is always a Bearer token, whatever its content
        assertEquals("Bearer login:jwt", service.authorizationValue(login(AUTH_URL, "user", "pass")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> payload = ArgumentCaptor.forClass(Map.class);
        verify(restTemplate).postForObject(eq(AUTH_URL), payload.capture(), eq(JsonNode.class));
        assertEquals(Map.of("type", "normal", "username", "user", "password", "pass"), payload.getValue());
    }

    @Test
    @DisplayName("the login token is reused until it is about to expire")
    void loginTokenIsCached() {
        when(restTemplate.postForObject(eq(AUTH_URL), any(), eq(JsonNode.class)))
                .thenReturn(loginResponse("tok1"), loginResponse("tok2"));
        TaigaContext context = login(AUTH_URL, "user", "pass");

        assertEquals("Bearer tok1", service.authorizationValue(context));
        setClock(NOW.plus(TaigaAuthService.LOGIN_TOKEN_TTL).minus(Duration.ofMinutes(5)));
        assertEquals("Bearer tok1", service.authorizationValue(context));
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(JsonNode.class));

        setClock(NOW.plus(TaigaAuthService.LOGIN_TOKEN_TTL).minus(Duration.ofSeconds(30)));
        assertEquals("Bearer tok2", service.authorizationValue(context));
        verify(restTemplate, times(2)).postForObject(anyString(), any(), eq(JsonNode.class));
    }

    @Test
    @DisplayName("the same user on two Taiga servers gets two tokens")
    void tokensAreKeptPerServer() {
        when(restTemplate.postForObject(eq(AUTH_URL), any(), eq(JsonNode.class))).thenReturn(loginResponse("tok_a"));
        when(restTemplate.postForObject(eq(OTHER_AUTH_URL), any(), eq(JsonNode.class)))
                .thenReturn(loginResponse("tok_b"));

        assertEquals("Bearer tok_a", service.authorizationValue(login(AUTH_URL, "user", "pass")));
        assertEquals("Bearer tok_b", service.authorizationValue(login(OTHER_AUTH_URL, "user", "pass")));
        assertEquals("Bearer tok_a", service.authorizationValue(login(AUTH_URL, "user", "pass")));
        verify(restTemplate, times(2)).postForObject(anyString(), any(), eq(JsonNode.class));
    }

    @Test
    @DisplayName("a changed password logs in again")
    void changedPasswordLogsInAgain() {
        when(restTemplate.postForObject(eq(AUTH_URL), any(), eq(JsonNode.class)))
                .thenReturn(loginResponse("tok1"), loginResponse("tok2"));

        assertEquals("Bearer tok1", service.authorizationValue(login(AUTH_URL, "user", "old")));
        assertEquals("Bearer tok2", service.authorizationValue(login(AUTH_URL, "user", "new")));
    }

    @Test
    @DisplayName("a rejected login is reported without the password")
    void rejectedLogin() {
        when(restTemplate.postForObject(eq(AUTH_URL), any(), eq(JsonNode.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        TaigaLoginException error = assertThrows(TaigaLoginException.class,
                () -> service.authorizationValue(login(AUTH_URL, "user", "s3cret-pass")));

        assertTrue(error.getMessage().contains(AUTH_URL));
        assertTrue(error.getMessage().contains("'user'"));
        assertTrue(!error.getMessage().contains("s3cret-pass"));
    }

    @Test
    @DisplayName("an unreachable Taiga server is reported as a login failure")
    void unreachableServer() {
        when(restTemplate.postForObject(eq(AUTH_URL), any(), eq(JsonNode.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThrows(TaigaLoginException.class, () -> service.authorizationValue(login(AUTH_URL, "user", "pass")));
    }

    @Test
    @DisplayName("a login response without auth_token is a login failure, and is not cached")
    void responseWithoutToken() {
        when(restTemplate.postForObject(eq(AUTH_URL), any(), eq(JsonNode.class)))
                .thenReturn(new ObjectMapper().createObjectNode(), loginResponse("tok"));
        TaigaContext context = login(AUTH_URL, "user", "pass");

        assertThrows(TaigaLoginException.class, () -> service.authorizationValue(context));
        assertEquals("Bearer tok", service.authorizationValue(context));
    }
}
