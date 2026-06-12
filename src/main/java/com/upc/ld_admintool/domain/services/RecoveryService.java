package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.domain.utils.DataSource;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.ProjectIdentityDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RecoveryService {

    @Value("${ld.connect.url}")
    private String ldConnectUrl;

    private final LDService ldService;
    private final RestTemplate restTemplate;

    public RecoveryService(LDService ldService) {
        this.ldService = ldService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRecoveryStatus(String jobId) {
        String statusUrl = ldConnectUrl + "/admin/recovery/status/" + jobId;
        ResponseEntity<Map> response = restTemplate.getForEntity(statusUrl, Map.class);
        return response.getBody() != null ? response.getBody() : Map.of("status", "error");
    }

    public Map<String, Object> runTeamRecovery(Long projectId, Map<String, String> tokenOverrides) {
        ProjectDTO project = ldService.getProjectById(projectId);
        if (project == null) throw new IllegalArgumentException("Project not found");

        String prj = project.getExternalId();
        if (prj == null || prj.isBlank()) throw new IllegalArgumentException("Project externalId is required");

        Map<DataSource, ProjectIdentityDTO> identities = project.getIdentities();
        String githubUrl = extractIdentityUrl(identities, DataSource.GITHUB);
        String taigaUrl = extractIdentityUrl(identities, DataSource.TAIGA);

        if (githubUrl == null || githubUrl.isBlank()) throw new IllegalArgumentException("Project GitHub identity URL is required");
        if (taigaUrl == null || taigaUrl.isBlank()) throw new IllegalArgumentException("Project Taiga identity URL is required");

        Map<String, Object> payload = new HashMap<>();
        payload.put("prj", prj);
        payload.put("github_url", githubUrl);
        payload.put("taiga_url", taigaUrl);
        applyTokenOverrides(payload, tokenOverrides, true, true);

        String jobId = startJob(ldConnectUrl + "/admin/recovery/team", payload);

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("projectExternalId", prj);
        result.put("job_id", jobId);
        result.put("status", "running");
        return result;
    }

    public Map<String, Object> runGithubRecovery(Long projectId, Map<String, String> tokenOverrides) {
        ProjectDTO project = ldService.getProjectById(projectId);
        if (project == null) throw new IllegalArgumentException("Project not found");

        String prj = project.getExternalId();
        if (prj == null || prj.isBlank()) throw new IllegalArgumentException("Project externalId is required");

        Map<DataSource, ProjectIdentityDTO> identities = project.getIdentities();
        String githubUrl = extractIdentityUrl(identities, DataSource.GITHUB);
        if (githubUrl == null || githubUrl.isBlank()) throw new IllegalArgumentException("Project GitHub identity URL is required");

        Map<String, Object> payload = new HashMap<>();
        payload.put("prj", prj);
        payload.put("github_url", githubUrl);
        applyTokenOverrides(payload, tokenOverrides, true, false);

        String jobId = startJob(ldConnectUrl + "/admin/recovery/github", payload);

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("projectExternalId", prj);
        result.put("job_id", jobId);
        result.put("status", "running");
        return result;
    }

    public Map<String, Object> runTaigaRecovery(Long projectId, Map<String, String> tokenOverrides) {
        ProjectDTO project = ldService.getProjectById(projectId);
        if (project == null) throw new IllegalArgumentException("Project not found");

        String prj = project.getExternalId();
        if (prj == null || prj.isBlank()) throw new IllegalArgumentException("Project externalId is required");

        Map<DataSource, ProjectIdentityDTO> identities = project.getIdentities();
        String taigaUrl = extractIdentityUrl(identities, DataSource.TAIGA);
        if (taigaUrl == null || taigaUrl.isBlank()) throw new IllegalArgumentException("Project Taiga identity URL is required");

        Map<String, Object> payload = new HashMap<>();
        payload.put("prj", prj);
        payload.put("taiga_url", taigaUrl);
        applyTokenOverrides(payload, tokenOverrides, false, true);

        String jobId = startJob(ldConnectUrl + "/admin/recovery/taiga", payload);

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("projectExternalId", prj);
        result.put("job_id", jobId);
        result.put("status", "running");
        return result;
    }

    @SuppressWarnings("unchecked")
    private String startJob(String url, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        return (String) response.getBody().get("job_id");
    }

    private void applyTokenOverrides(Map<String, Object> payload, Map<String, String> overrides,
                                      boolean github, boolean taiga) {
        if (overrides == null) return;
        if (github) {
            String t = overrides.get("githubToken");
            if (t != null && !t.isBlank()) payload.put("github_token", t);
        }
        if (taiga) {
            String t = overrides.get("taigaToken");
            if (t != null && !t.isBlank()) payload.put("taiga_token", t);
        }
    }

    private String extractIdentityUrl(Map<DataSource, ProjectIdentityDTO> identities, DataSource source) {
        if (identities == null) return null;
        ProjectIdentityDTO identity = identities.get(source);
        return identity != null ? identity.getUrl() : null;
    }
}
