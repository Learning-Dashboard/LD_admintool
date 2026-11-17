package com.upc.ld_admintool.domain.services.credentials;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the Authorization header value for a subject's Taiga server: from the
 * static token of the context, or from a login at the context's auth URL.
 */
@Service
public class TaigaAuthService {

    /** Taiga login tokens expire after 24 hours. */
    static final Duration LOGIN_TOKEN_TTL = Duration.ofHours(23);
    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofSeconds(60);

    private final RestTemplate restTemplate;
    private final Map<LoginKey, LoginToken> loginTokens = new ConcurrentHashMap<>();
    private Clock clock = Clock.systemUTC();

    /** One login per Taiga server and user; the password is part of the key so a changed password logs in again. */
    private record LoginKey(String authUrl, String username, String password) {
    }

    private record LoginToken(String token, Instant expiry) {
    }

    public TaigaAuthService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * @throws TaigaLoginException if the context has no static token and the
     *                             login fails
     */
    public String authorizationValue(TaigaContext context) {
        if (context.hasToken()) {
            return staticTokenAuthorization(context.token());
        }
        return "Bearer " + loginToken(context);
    }

    /**
     * Application tokens contain colons and must use the Application scheme.
     * Access JWTs have no colons and keep Bearer. A saved "Application " or
     * "Bearer " prefix is stripped once so it is not sent twice.
     */
    static String staticTokenAuthorization(String token) {
        String raw = token == null ? "" : token.trim();
        for (String scheme : new String[] {"Application", "Bearer"}) {
            // A scheme with nothing after it is an empty token, not a token named like the scheme.
            if (raw.equals(scheme) || raw.startsWith(scheme + " ")) {
                raw = raw.substring(scheme.length()).trim();
                break;
            }
        }
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("The Taiga token is empty");
        }
        String scheme = raw.contains(":") ? "Application" : "Bearer";
        return scheme + " " + raw;
    }

    private String loginToken(TaigaContext context) {
        LoginKey key = new LoginKey(context.authUrl(), context.username(), context.password());
        Instant now = clock.instant();

        LoginToken cached = loginTokens.get(key);
        if (cached != null && now.isBefore(cached.expiry().minus(RENEW_BEFORE_EXPIRY))) {
            return cached.token();
        }

        String token = login(context);
        loginTokens.put(key, new LoginToken(token, now.plus(LOGIN_TOKEN_TTL)));
        return token;
    }

    private String login(TaigaContext context) {
        Map<String, String> payload = Map.of(
                "type", "normal",
                "username", context.username(),
                "password", context.password());

        JsonNode response;
        try {
            response = restTemplate.postForObject(context.authUrl(), payload, JsonNode.class);
        } catch (RestClientException e) {
            throw new TaigaLoginException("Taiga login failed at " + context.authUrl() + " for user '"
                    + context.username() + "': " + e.getMessage(), e);
        }

        JsonNode token = response == null ? null : response.get("auth_token");
        if (token == null || !token.isTextual() || token.asText().isEmpty()) {
            throw new TaigaLoginException("Taiga login at " + context.authUrl() + " for user '"
                    + context.username() + "' did not return an auth_token.", null);
        }
        return token.asText();
    }
}
