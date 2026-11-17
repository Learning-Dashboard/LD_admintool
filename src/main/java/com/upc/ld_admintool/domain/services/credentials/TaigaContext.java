package com.upc.ld_admintool.domain.services.credentials;

/**
 * Taiga server and credentials that apply to one subject.
 *
 * Either {@code token} is set (static token) or {@code username} and
 * {@code password} are set (login at {@code authUrl}).
 */
public record TaigaContext(String apiUrl, String authUrl, String token, String username, String password) {

    public static TaigaContext withToken(String apiUrl, String authUrl, String token) {
        return new TaigaContext(apiUrl, authUrl, token, "", "");
    }

    public static TaigaContext withLogin(String apiUrl, String authUrl, String username, String password) {
        return new TaigaContext(apiUrl, authUrl, "", username, password);
    }

    public boolean hasToken() {
        return token != null && !token.isEmpty();
    }

    /** Secrets are left out so that a context can be logged. */
    @Override
    public String toString() {
        return "TaigaContext[apiUrl=" + apiUrl + ", authUrl=" + authUrl + ", username=" + username + "]";
    }
}
