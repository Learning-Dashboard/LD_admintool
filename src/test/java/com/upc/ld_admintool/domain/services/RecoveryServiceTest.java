package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.domain.utils.DataSource;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.ProjectIdentityDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The recovery requests carry no credentials: LD Connect resolves them from the
 * team (prj) in its credentials config.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecoveryService")
class RecoveryServiceTest {

    private static final String LD_CONNECT_URL = "http://ld-connect:5000";
    private static final String GITHUB_URL = "https://github.com/testorg";
    private static final String TAIGA_URL = "https://tree.taiga.io/project/testuser-testproject/";

    @Mock
    private LDService ldService;

    @Mock
    private RestTemplate restTemplate;

    private RecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new RecoveryService(ldService);
        ReflectionTestUtils.setField(recoveryService, "ldConnectUrl", LD_CONNECT_URL);
        ReflectionTestUtils.setField(recoveryService, "restTemplate", restTemplate);

        lenient().when(ldService.getProjectById(1L)).thenReturn(project(GITHUB_URL, TAIGA_URL));
        lenient().when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("job_id", "job-1", "status", "running"), HttpStatus.ACCEPTED));
    }

    private static ProjectDTO project(String githubUrl, String taigaUrl) {
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setExternalId("test-project");
        project.setSubject("AMEP");

        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        if (githubUrl != null) {
            ProjectIdentityDTO github = new ProjectIdentityDTO();
            github.setUrl(githubUrl);
            identities.put(DataSource.GITHUB, github);
        }
        if (taigaUrl != null) {
            ProjectIdentityDTO taiga = new ProjectIdentityDTO();
            taiga.setUrl(taigaUrl);
            identities.put(DataSource.TAIGA, taiga);
        }
        project.setIdentities(identities);
        return project;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payloadSentTo(String path) {
        ArgumentCaptor<HttpEntity<Map<String, Object>>> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(LD_CONNECT_URL + path), request.capture(), eq(Map.class));
        return request.getValue().getBody();
    }

    @Test
    @DisplayName("team recovery sends only the team and its URLs to LD Connect")
    void teamRecoveryPayload() {
        Map<String, Object> result = recoveryService.runTeamRecovery(1L);

        assertEquals(Map.of("prj", "test-project", "github_url", GITHUB_URL, "taiga_url", TAIGA_URL),
                payloadSentTo("/admin/recovery/team"));
        assertEquals("job-1", result.get("job_id"));
        assertEquals("running", result.get("status"));
        assertEquals("test-project", result.get("projectExternalId"));
    }

    @Test
    @DisplayName("GitHub recovery sends only the team and its GitHub URL")
    void githubRecoveryPayload() {
        recoveryService.runGithubRecovery(1L);

        assertEquals(Map.of("prj", "test-project", "github_url", GITHUB_URL),
                payloadSentTo("/admin/recovery/github"));
    }

    @Test
    @DisplayName("Taiga recovery sends only the team and its Taiga URL")
    void taigaRecoveryPayload() {
        recoveryService.runTaigaRecovery(1L);

        assertEquals(Map.of("prj", "test-project", "taiga_url", TAIGA_URL),
                payloadSentTo("/admin/recovery/taiga"));
    }

    @Test
    @DisplayName("an unknown project is rejected before calling LD Connect")
    void unknownProject() {
        when(ldService.getProjectById(2L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> recoveryService.runTeamRecovery(2L));
        assertThrows(IllegalArgumentException.class, () -> recoveryService.runGithubRecovery(2L));
        assertThrows(IllegalArgumentException.class, () -> recoveryService.runTaigaRecovery(2L));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("a project without the identity URL it needs is rejected")
    void missingIdentityUrl() {
        when(ldService.getProjectById(1L)).thenReturn(project(GITHUB_URL, null));
        assertThrows(IllegalArgumentException.class, () -> recoveryService.runTeamRecovery(1L));
        assertThrows(IllegalArgumentException.class, () -> recoveryService.runTaigaRecovery(1L));

        when(ldService.getProjectById(1L)).thenReturn(project(null, TAIGA_URL));
        assertThrows(IllegalArgumentException.class, () -> recoveryService.runGithubRecovery(1L));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("a project without externalId is rejected")
    void missingExternalId() {
        ProjectDTO project = project(GITHUB_URL, TAIGA_URL);
        project.setExternalId(" ");
        when(ldService.getProjectById(1L)).thenReturn(project);

        assertThrows(IllegalArgumentException.class, () -> recoveryService.runTeamRecovery(1L));
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("the job status is read from LD Connect")
    void recoveryStatus() {
        when(restTemplate.getForEntity(LD_CONNECT_URL + "/admin/recovery/status/job-1", Map.class))
                .thenReturn(new ResponseEntity<>(Map.of("status", "done"), HttpStatus.OK));

        assertEquals(Map.of("status", "done"), recoveryService.getRecoveryStatus("job-1"));
    }
}
