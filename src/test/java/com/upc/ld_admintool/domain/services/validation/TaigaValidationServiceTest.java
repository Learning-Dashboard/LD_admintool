package com.upc.ld_admintool.domain.services.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.ld_admintool.domain.services.credentials.TaigaAuthService;
import com.upc.ld_admintool.domain.services.credentials.TaigaContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para TaigaValidationService
 * Valida la lógica de validación de proyectos y usuarios en Taiga
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaigaValidationService - Tests Unitarios")
class TaigaValidationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TaigaValidationService taigaValidationService;

    private static final String TAIGA_API_URL = "https://taiga.subject.example/api/v1";
    private static final String TAIGA_AUTH_URL = "https://taiga.subject.example/api/v1/auth";
    private static final String TAIGA_TOKEN = "test-token";

    /** Taiga server and credentials of the subject of the project under validation. */
    private TaigaContext taiga;
    private TaigaAuthService taigaAuthService;

    @BeforeEach
    void setUp() {
        taiga = TaigaContext.withToken(TAIGA_API_URL, TAIGA_AUTH_URL, TAIGA_TOKEN);
        taigaAuthService = new TaigaAuthService();
        ReflectionTestUtils.setField(taigaValidationService, "taigaAuthService", taigaAuthService);
        ReflectionTestUtils.setField(taigaValidationService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(taigaValidationService, "objectMapper", new ObjectMapper());
    }

    @Test
    @DisplayName("validateProjectBySlug debe validar proyecto existente correctamente")
    void testValidateProjectBySlug_Success() {
        // Arrange
        String projectSlug = "test-project";
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"id\":1,\"name\":\"Test\"}", HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(projectSlug, taiga);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar error si proyecto no existe")
    void testValidateProjectBySlug_NotFound() {
        // Arrange
        String projectSlug = "non-existent-project";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(projectSlug, taiga);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar error si slug está vacío")
    void testValidateProjectBySlug_EmptySlug() {
        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug("", taiga);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get(0).contains("buit"));
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar error si slug es null")
    void testValidateProjectBySlug_NullSlug() {
        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(null, taiga);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar error de autorización")
    void testValidateProjectBySlug_Unauthorized() {
        // Arrange
        String projectSlug = "private-project";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.Unauthorized.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(projectSlug, taiga);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar warning si proyecto es privado")
    void testValidateProjectBySlug_Forbidden() {
        // Arrange
        String projectSlug = "private-project";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.Forbidden.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null));

        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(projectSlug, taiga);

        // Assert
        assertTrue(result.isValid());
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    @DisplayName("validateUsersInProject debe validar usuarios correctamente")
    void testValidateUsersInProject_Success() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1", "user2");
        
        String jsonResponse = "{\"members\":[" +
                "{\"username\":\"user1\"}," +
                "{\"username\":\"user2\"}," +
                "{\"username\":\"user3\"}" +
                "]}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames, taiga);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe retornar error si usuario no es miembro")
    void testValidateUsersInProject_UserNotMember() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1", "nonmember");
        
        String jsonResponse = "{\"members\":[{\"username\":\"user1\"}]}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames, taiga);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe retornar válido si lista de usuarios está vacía")
    void testValidateUsersInProject_EmptyList() {
        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject("test-project", Arrays.asList(), taiga);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe retornar válido si lista es null")
    void testValidateUsersInProject_NullList() {
        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject("test-project", null, taiga);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe retornar warning si no hay members disponibles")
    void testValidateUsersInProject_NoMembers() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1");
        
        String jsonResponse = "{\"id\":1,\"name\":\"Test\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames, taiga);

        // Assert - El código añade warning, no error
        assertTrue(result.isValid());
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    @DisplayName("validateUsersInProject debe manejar error HTTP")
    void testValidateUsersInProject_HttpError() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1");
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames, taiga);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe manejar JSON inválido")
    void testValidateUsersInProject_InvalidJson() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1");
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>("invalid json", HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames, taiga);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("un token de aplicación con dos puntos usa el esquema Application")
    void testAuthorization_ApplicationToken() {
        String applicationToken = "payload:timestamp:signature";
        taiga = TaigaContext.withToken(TAIGA_API_URL, TAIGA_AUTH_URL, applicationToken);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"id\":1}", HttpStatus.OK));

        taigaValidationService.validateProjectBySlug("supertest", taiga);

        assertEquals("Application " + applicationToken, capturedAuthorization());
    }

    @Test
    @DisplayName("la validación de usuarios envía el mismo esquema Application")
    void testAuthorization_UsersUseSameHelper() {
        String applicationToken = "payload:timestamp:signature";
        taiga = TaigaContext.withToken(TAIGA_API_URL, TAIGA_AUTH_URL, applicationToken);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"members\":[]}", HttpStatus.OK));

        taigaValidationService.validateUsersInProject("supertest", Arrays.asList("user1"), taiga);

        assertEquals("Application " + applicationToken, capturedAuthorization());
    }

    @Test
    @DisplayName("un JWT sin dos puntos sigue usando Bearer")
    void testAuthorization_AccessJwtKeepsBearer() {
        String accessToken = "eyJhbGciOiJIUzI1NiJ9.payload.signature";
        taiga = TaigaContext.withToken(TAIGA_API_URL, TAIGA_AUTH_URL, "Bearer " + accessToken);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"id\":1}", HttpStatus.OK));

        taigaValidationService.validateProjectBySlug("supertest", taiga);

        assertEquals("Bearer " + accessToken, capturedAuthorization());
    }

    @Test
    @DisplayName("las peticiones van al servidor Taiga de la asignatura")
    void testRequestsGoToSubjectServer() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"id\":1}", HttpStatus.OK));

        taigaValidationService.validateProjectBySlug("supertest", taiga);
        taigaValidationService.validateProjectBySlug("supertest",
                TaigaContext.withToken("https://api.taiga.io/api/v1", "https://api.taiga.io/api/v1/auth", "tok"));

        verify(restTemplate).exchange(eq(TAIGA_API_URL + "/projects/by_slug?slug=supertest"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class));
        verify(restTemplate).exchange(eq("https://api.taiga.io/api/v1/projects/by_slug?slug=supertest"),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("sin token se hace login en el auth URL de la asignatura")
    void testAuthorization_LoginWhenNoToken() {
        RestTemplate loginRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(taigaAuthService, "restTemplate", loginRestTemplate);
        when(loginRestTemplate.postForObject(eq(TAIGA_AUTH_URL), any(), eq(JsonNode.class)))
                .thenReturn(new ObjectMapper().createObjectNode().put("auth_token", "login-jwt"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"id\":1}", HttpStatus.OK));

        ValidationResult result = taigaValidationService.validateProjectBySlug("supertest",
                TaigaContext.withLogin(TAIGA_API_URL, TAIGA_AUTH_URL, "user", "pass"));

        assertTrue(result.isValid());
        assertEquals("Bearer login-jwt", capturedAuthorization());
    }

    @Test
    @DisplayName("si el login falla no se hace ninguna petición anónima")
    void testAuthorization_LoginFailure() {
        RestTemplate loginRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(taigaAuthService, "restTemplate", loginRestTemplate);
        when(loginRestTemplate.postForObject(eq(TAIGA_AUTH_URL), any(), eq(JsonNode.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));
        TaigaContext login = TaigaContext.withLogin(TAIGA_API_URL, TAIGA_AUTH_URL, "user", "pass");

        ValidationResult projectResult = taigaValidationService.validateProjectBySlug("supertest", login);
        ValidationResult usersResult = taigaValidationService.validateUsersInProject("supertest",
                Arrays.asList("user1"), login);

        assertFalse(projectResult.isValid());
        assertTrue(projectResult.getErrors().get(0).contains("Taiga login failed at " + TAIGA_AUTH_URL));
        assertFalse(usersResult.isValid());
        assertTrue(usersResult.getErrors().get(0).contains("Taiga login failed at " + TAIGA_AUTH_URL));
        verifyNoInteractions(restTemplate);
    }

    @SuppressWarnings("unchecked")
    private String capturedAuthorization() {
        ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), entityCaptor.capture(), eq(String.class));
        return entityCaptor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    }
}
