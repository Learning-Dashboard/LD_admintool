package com.upc.ld_admintool.domain.services.validation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

@Service
public class GitHubValidationService {

    @Value("${github.api.url:https://api.github.com}")
    private String githubApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Valida si una organització de GitHub existeix.
     * 
     * @param org         Nom de l'organització
     * @param githubToken Token de GitHub de l'assignatura del projecte
     * @return ValidationResult amb errors si l'organització no existeix
     */
    public ValidationResult validateOrganization(String org, String githubToken) {
        ValidationResult result = new ValidationResult(true);

        if (org == null || org.trim().isEmpty()) {
            result.addError("El nom de l'organització GitHub està buit");
            return result;
        }

        try {
            String url = githubApiUrl + "/orgs/" + org + "/members";
            HttpHeaders headers = createHeaders(githubToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Object[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, Object[].class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Object[] members = response.getBody();
                if (members == null || members.length == 0) {
                    // Organització existeix però membres són privats
                    result.addWarning("L'organització '" + org + "' té membres privats (no es poden validar usuaris)");  
                }
            }

        } catch (HttpClientErrorException.NotFound e) {
            result.addError("L'organització GitHub '" + org + "' no existeix");
        } catch (HttpClientErrorException.Unauthorized e) {
            result.addError("No tens autorització per accedir a l'organització '" + org + "' (comprova el token)");
        } catch (Exception e) {
            result.addError("Error validant l'organització GitHub '" + org + "': " + e.getMessage());
        }

        return result;
    }

    /**
     * Valida si múltiples usuaris són membres d'una organització.
     * 
     * @param org         Nom de l'organització
     * @param usernames   Llista de noms d'usuari
     * @param githubToken Token de GitHub de l'assignatura del projecte
     * @return ValidationResult amb errors per cada usuari que no és membre
     */
    public ValidationResult validateUsersInOrganization(String org, List<String> usernames, String githubToken) {
        ValidationResult result = new ValidationResult(true);

        if (usernames == null || usernames.isEmpty()) {
            return result;
        }

        try {
            String url = githubApiUrl + "/orgs/" + org + "/members";
            HttpHeaders headers = createHeaders(githubToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Object[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, Object[].class);

            Object[] members = response.getBody();
            if (members == null || members.length == 0) {
                // No podem validar perquè membres són privats - això és un ERROR
                result.addError("No es poden validar els usuaris perquè els membres de l'organització '" + org
                        + "' són privats. Proporciona un token amb permisos adequats.");
                return result;
            }

            // Comprovar cada usuari (com fa existsUsername)
            for (String username : usernames) {
                if (username == null || username.trim().isEmpty()) {
                    continue;
                }

                boolean found = false;
                for (Object memberObj : members) {
                    if (memberObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> member = (Map<String, Object>) memberObj;
                        String login = (String) member.get("login");

                        if (username.trim().equals(login != null ? login.trim() : "")) {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    result.addError("L'usuari GitHub '" + username + "' no és membre de l'organització '" + org + "'");
                }
            }

        } catch (HttpClientErrorException.NotFound e) {
            result.addError("L'organització '" + org + "' no existeix");
        } catch (Exception e) {
            result.addError("Error validant usuaris a l'organització: " + e.getMessage());
        }

        return result;
    }

    /**
     * Valida si un usuari de GitHub existeix.
     * 
     * @param username    Nom d'usuari
     * @param githubToken Token de GitHub de l'assignatura del projecte
     * @return ValidationResult amb errors si l'usuari no existeix
     */
    public ValidationResult validateUser(String username, String githubToken) {
        ValidationResult result = new ValidationResult(true);

        try {
            String url = githubApiUrl + "/users/" + username;
            HttpHeaders headers = createHeaders(githubToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                result.addError("L'usuari GitHub '" + username + "' no existeix");
            }
        } catch (HttpClientErrorException.NotFound e) {
            result.addError("L'usuari GitHub '" + username + "' no existeix");
        } catch (Exception e) {
            result.addError("Error validant l'usuari GitHub '" + username + "': " + e.getMessage());
        }

        return result;
    }

    /**
     * Crea headers HTTP per les peticions a GitHub API.
     * 
     * @param githubToken Token de GitHub de l'assignatura del projecte. No hi ha
     *                    token global: sense token no es fa cap petició.
     * @return HttpHeaders amb el token
     */
    private HttpHeaders createHeaders(String githubToken) {
        if (githubToken == null || githubToken.trim().isEmpty()) {
            throw new IllegalArgumentException("The GitHub token of the subject is empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("Authorization", "token " + githubToken.trim());
        return headers;
    }
}
