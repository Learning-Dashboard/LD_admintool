package com.upc.ld_admintool.domain.services.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaigaValidationService {

    @Value("${taiga.api.url:https://api.taiga.io/api/v1}")
    private String taigaApiUrl;

    @Value("${taiga.token:}")
    private String taigaToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Valida si un projecte de Taiga existeix per slug
     */
    public ValidationResult validateProjectBySlug(String projectSlug) {
        ValidationResult result = new ValidationResult(true);

        if (projectSlug == null || projectSlug.trim().isEmpty()) {
            result.addError("El slug del projecte Taiga està buit");
            return result;
        }

        try {
            String url = taigaApiUrl + "/projects/by_slug?slug=" + projectSlug;
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        } catch (HttpClientErrorException.NotFound e) {
            result.addError("El projecte Taiga '" + projectSlug + "' no existeix");
        } catch (HttpClientErrorException.Unauthorized e) {
            result.addError("No tens autorització per accedir al projecte Taiga '" + projectSlug
                    + "' (comprova el token de Taiga)");
        } catch (HttpClientErrorException.Forbidden e) {
            result.addWarning("El projecte Taiga '" + projectSlug + "' és privat o no tens permisos");
        } catch (Exception e) {
            result.addError("Error validant el projecte Taiga '" + projectSlug + "': " + e.getMessage());
        }

        return result;
    }

    /**
     * Valida si els usuaris existeixen com a membres del projecte Taiga
     */
    public ValidationResult validateUsersInProject(String projectSlug, List<String> usernames) {
        ValidationResult result = new ValidationResult(true);

        if (usernames == null || usernames.isEmpty()) {
            return result;
        }

        try {
            String url = taigaApiUrl + "/projects/by_slug?slug=" + projectSlug;
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse JSON per extreure els membres
                JsonNode projectData = objectMapper.readTree(response.getBody());
                JsonNode membersNode = projectData.get("members");

                if (membersNode != null && membersNode.isArray()) {
                    // Extreure usernames dels membres
                    Set<String> projectMembers = new HashSet<>();
                    for (JsonNode memberNode : membersNode) {
                        JsonNode usernameNode = memberNode.get("username");
                        if (usernameNode != null) {
                            projectMembers.add(usernameNode.asText()); // Validation is now strict case-sensitive
                        }
                    }

                    // Validar cada username
                    for (String username : usernames) {
                        if (username == null || username.trim().isEmpty()) {
                            result.addWarning("Hi ha un username buit a la llista");
                            continue;
                        }

                        if (projectMembers.contains(username.trim())) {
                            System.out.println(
                                    "✅ Usuari '" + username + "' és membre del projecte Taiga '" + projectSlug + "'");
                        } else {
                            result.addError("L'usuari '" + username + "' NO és membre del projecte Taiga '"
                                    + projectSlug + "'");
                        }
                    }
                } else {
                    result.addWarning("No es poden validar els membres del projecte Taiga '" + projectSlug
                            + "' (membres no disponibles)");
                }
            } else {
                result.addError("No es pot accedir al projecte Taiga '" + projectSlug + "' per validar usuaris");
            }

        } catch (HttpClientErrorException.NotFound e) {
            result.addError("El projecte Taiga '" + projectSlug + "' no existeix");
        } catch (HttpClientErrorException.Unauthorized e) {
            result.addError("No tens autorització per accedir al projecte Taiga '" + projectSlug
                    + "' (comprova el token de Taiga)");
        } catch (HttpClientErrorException.Forbidden e) {
            result.addWarning(
                    "El projecte Taiga '" + projectSlug + "' és privat o no tens permisos per veure els membres");
        } catch (Exception e) {
            result.addError("Error validant usuaris al projecte Taiga '" + projectSlug + "': " + e.getMessage());
        }

        return result;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        if (taigaToken != null && !taigaToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + taigaToken);
        }
        return headers;
    }
}
