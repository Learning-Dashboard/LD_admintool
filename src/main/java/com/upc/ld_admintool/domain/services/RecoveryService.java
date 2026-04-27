package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.domain.utils.DataSource;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.ProjectIdentityDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RecoveryService {

    @Value("${ld.connect.url}")
    private String ldConnectUrl;

    private final LDService ldService;
    private final RestTemplate restTemplate = new RestTemplate();

    public RecoveryService(LDService ldService) {
        this.ldService = ldService;
    }

    public Map<String, Object> runTeamRecovery(Long projectId, Map<String, String> tokenOverrides) {
        ProjectDTO project = ldService.getProjectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project not found");
        }

        String prj = project.getExternalId();
        if (prj == null || prj.isBlank()) {
            throw new IllegalArgumentException("Project externalId is required");
        }

        Map<DataSource, ProjectIdentityDTO> identities = project.getIdentities();
        String githubUrl = extractIdentityUrl(identities, DataSource.GITHUB);
        String taigaUrl = extractIdentityUrl(identities, DataSource.TAIGA);

        if (githubUrl == null || githubUrl.isBlank()) {
            throw new IllegalArgumentException("Project GitHub identity URL is required");
        }
        if (taigaUrl == null || taigaUrl.isBlank()) {
            throw new IllegalArgumentException("Project Taiga identity URL is required");
        }

        String url = ldConnectUrl + "/admin/recovery/team";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("prj", prj);
        payload.put("github_url", githubUrl);
        payload.put("taiga_url", taigaUrl);

        if (tokenOverrides != null) {
            String githubToken = tokenOverrides.get("githubToken");
            String taigaToken = tokenOverrides.get("taigaToken");

            if (githubToken != null && !githubToken.isBlank()) {
                payload.put("github_token", githubToken);
            }
            if (taigaToken != null && !taigaToken.isBlank()) {
                payload.put("taiga_token", taigaToken);
            }
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("projectExternalId", prj);
        result.put("recovery", response.getBody());
        return result;
    }

    private String extractIdentityUrl(Map<DataSource, ProjectIdentityDTO> identities, DataSource source) {
        if (identities == null) {
            return null;
        }
        ProjectIdentityDTO identity = identities.get(source);
        return identity != null ? identity.getUrl() : null;
    }
}
