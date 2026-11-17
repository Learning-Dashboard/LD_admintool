package com.upc.ld_admintool.domain.services.validation;

import com.upc.ld_admintool.domain.services.LDService;
import com.upc.ld_admintool.domain.services.credentials.CredentialsConfigException;
import com.upc.ld_admintool.domain.services.credentials.SubjectCredentials;
import com.upc.ld_admintool.domain.services.credentials.SubjectCredentialsService;
import com.upc.ld_admintool.domain.services.credentials.TaigaContext;
import com.upc.ld_admintool.domain.utils.DataSource;
import com.upc.ld_admintool.rest.DTO.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para ProjectValidationService
 * Valida la funcionalidad de validación de proyectos y estudiantes
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectValidationService - Tests Unitarios")
class ProjectValidationServiceTest {

    @Mock
    private GitHubValidationService githubValidationService;

    @Mock
    private TaigaValidationService taigaValidationService;

    @Mock
    private LDService ldService;

    @Mock
    private SubjectCredentialsService subjectCredentialsService;

    @InjectMocks
    private ProjectValidationService projectValidationService;

    private static final String SUBJECT = "AMEP";
    private static final String GITHUB_TOKEN = "github-token";
    private static final TaigaContext TAIGA = TaigaContext.withToken(
            "https://taiga.amep.example/api/v1", "https://taiga.amep.example/api/v1/auth", "taiga-token");
    /** Credentials config entry of the subject: both test teams are listed under it. */
    private static final SubjectCredentials CREDENTIALS = new SubjectCredentials(
            SUBJECT, GITHUB_TOKEN, TAIGA, List.of("test-project", "project-2"));

    private static final String OTHER_SUBJECT = "ASW";
    private static final TaigaContext OTHER_TAIGA = TaigaContext.withLogin(
            "https://api.taiga.io/api/v1", "https://api.taiga.io/api/v1/auth", "fallback_user", "fallback_pass");
    private static final SubjectCredentials OTHER_CREDENTIALS = new SubjectCredentials(
            OTHER_SUBJECT, "other-github-token", OTHER_TAIGA, List.of("asw-team"));

    private ProjectDTO createValidProject() {
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setName("Test Project");
        project.setExternalId("test-project");
        project.setSubject(SUBJECT);

        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        
        ProjectIdentityDTO githubIdentity = new ProjectIdentityDTO();
        githubIdentity.setUrl("https://github.com/testorg");
        identities.put(DataSource.GITHUB, githubIdentity);
        
        ProjectIdentityDTO taigaIdentity = new ProjectIdentityDTO();
        taigaIdentity.setUrl("https://tree.taiga.io/project/testuser-testproject/");
        identities.put(DataSource.TAIGA, taigaIdentity);
        
        project.setIdentities(identities);
        
        return project;
    }

    private StudentDTO createStudent(String name, String githubUsername, String taigaUsername) {
        StudentDTO student = new StudentDTO();
        student.setName(name);
        
        Map<DataSource, StudentIdentityDTO> identities = new HashMap<>();
        
        if (githubUsername != null) {
            StudentIdentityDTO githubIdentity = new StudentIdentityDTO();
            githubIdentity.setUsername(githubUsername);
            identities.put(DataSource.GITHUB, githubIdentity);
        }
        
        if (taigaUsername != null) {
            StudentIdentityDTO taigaIdentity = new StudentIdentityDTO();
            taigaIdentity.setUsername(taigaUsername);
            identities.put(DataSource.TAIGA, taigaIdentity);
        }
        
        student.setIdentities(identities);
        return student;
    }

    /** A valid project whose identities are replaced by the given ones. */
    private ProjectDTO createProjectWithIdentities(Map<DataSource, ProjectIdentityDTO> identities) {
        ProjectDTO project = createValidProject();
        project.setIdentities(identities);
        return project;
    }

    @BeforeEach
    void setUp() {
        lenient().when(subjectCredentialsService.resolveBySubject(SUBJECT)).thenReturn(CREDENTIALS);
        lenient().when(subjectCredentialsService.resolveBySubject(OTHER_SUBJECT)).thenReturn(OTHER_CREDENTIALS);
        lenient().when(ldService.getProjectById(1L)).thenAnswer(invocation -> createValidProject());

        lenient().when(githubValidationService.validateOrganization(anyString(), anyString()))
                .thenReturn(new ValidationResult(true));
        lenient().when(githubValidationService.validateUsersInOrganization(anyString(), anyList(), anyString()))
                .thenReturn(new ValidationResult(true));
        lenient().when(taigaValidationService.validateProjectBySlug(anyString(), any(TaigaContext.class)))
                .thenReturn(new ValidationResult(true));
        lenient().when(taigaValidationService.validateUsersInProject(anyString(), anyList(), any(TaigaContext.class)))
                .thenReturn(new ValidationResult(true));
    }

    @Test
    @DisplayName("validateProject debe validar un proyecto válido")
    void testValidateProject_ValidProject() {
        // Arrange
        ProjectDTO project = createValidProject();

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        verify(githubValidationService).validateOrganization("testorg", GITHUB_TOKEN);
        verify(taigaValidationService).validateProjectBySlug("testuser-testproject", TAIGA);
    }

    @Test
    @DisplayName("validateProject debe retornar error si no tiene identidades")
    void testValidateProject_NoIdentities() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setName("Test Project");
        project.setIdentities(null);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get(0).contains("does not have defined identities"));
    }

    @Test
    @DisplayName("validateProject debe retornar error si identidades está vacío")
    void testValidateProject_EmptyIdentities() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setName("Test Project");
        project.setIdentities(new HashMap<>());

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateProject debe agregar warning si no tiene URL de GitHub")
    void testValidateProject_NoGitHubURL() {
        // Arrange
        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        ProjectIdentityDTO taigaIdentity = new ProjectIdentityDTO();
        taigaIdentity.setUrl("https://tree.taiga.io/project/test/");
        identities.put(DataSource.TAIGA, taigaIdentity);

        ProjectDTO project = createProjectWithIdentities(identities);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().stream()
                .anyMatch(w -> w.contains("does not have a defined GitHub URL")));
    }

    @Test
    @DisplayName("validateProject debe agregar warning si no tiene URL de Taiga")
    void testValidateProject_NoTaigaURL() {
        // Arrange
        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        ProjectIdentityDTO githubIdentity = new ProjectIdentityDTO();
        githubIdentity.setUrl("https://github.com/testorg");
        identities.put(DataSource.GITHUB, githubIdentity);

        ProjectDTO project = createProjectWithIdentities(identities);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().stream()
                .anyMatch(w -> w.contains("does not have a defined Taiga URL")));
    }

    @Test
    @DisplayName("validateProject debe validar estudiantes en GitHub")
    void testValidateProject_WithGitHubStudents() {
        // Arrange
        ProjectDTO project = createValidProject();
        List<StudentDTO> students = Arrays.asList(
                createStudent("Student 1", "github1", "taiga1"),
                createStudent("Student 2", "github2", "taiga2")
        );
        project.setStudents(students);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertTrue(result.isValid());
        verify(githubValidationService).validateUsersInOrganization(
                eq("testorg"),
                argThat(list -> list.size() == 2 && list.contains("github1") && list.contains("github2")),
                eq(GITHUB_TOKEN)
        );
    }

    @Test
    @DisplayName("validateProject debe validar estudiantes en Taiga")
    void testValidateProject_WithTaigaStudents() {
        // Arrange
        ProjectDTO project = createValidProject();
        List<StudentDTO> students = Arrays.asList(
                createStudent("Student 1", "github1", "taiga1"),
                createStudent("Student 2", "github2", "taiga2")
        );
        project.setStudents(students);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertTrue(result.isValid());
        verify(taigaValidationService).validateUsersInProject(
                eq("testuser-testproject"),
                argThat(list -> list.size() == 2 && list.contains("taiga1") && list.contains("taiga2")),
                eq(TAIGA)
        );
    }

    @Test
    @DisplayName("validateProject debe manejar URL de GitHub inválida")
    void testValidateProject_InvalidGitHubURL() {
        // Arrange
        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        ProjectIdentityDTO githubIdentity = new ProjectIdentityDTO();
        githubIdentity.setUrl("invalid-url");
        identities.put(DataSource.GITHUB, githubIdentity);

        ProjectDTO project = createProjectWithIdentities(identities);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("Invalid GitHub URL format")));
    }

    @Test
    @DisplayName("validateProject debe manejar URL de Taiga inválida")
    void testValidateProject_InvalidTaigaURL() {
        // Arrange
        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        ProjectIdentityDTO taigaIdentity = new ProjectIdentityDTO();
        taigaIdentity.setUrl("invalid-taiga-url");
        identities.put(DataSource.TAIGA, taigaIdentity);

        ProjectDTO project = createProjectWithIdentities(identities);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("Invalid Taiga URL format")));
    }

    @Test
    @DisplayName("validateProject debe propagar errores de GitHub validation")
    void testValidateProject_GitHubValidationErrors() {
        // Arrange
        ProjectDTO project = createValidProject();
        ValidationResult githubError = new ValidationResult(false);
        githubError.addError("GitHub organization not found");
        
        when(githubValidationService.validateOrganization(anyString(), anyString()))
                .thenReturn(githubError);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("GitHub organization not found"));
    }

    @Test
    @DisplayName("validateProject debe propagar errores de Taiga validation")
    void testValidateProject_TaigaValidationErrors() {
        // Arrange
        ProjectDTO project = createValidProject();
        ValidationResult taigaError = new ValidationResult(false);
        taigaError.addError("Taiga project not found");
        
        when(taigaValidationService.validateProjectBySlug(anyString(), any(TaigaContext.class)))
                .thenReturn(taigaError);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Taiga project not found"));
    }

    @Test
    @DisplayName("validateProjectsWithDetails debe validar múltiples proyectos")
    void testValidateProjectsWithDetails_MultipleProjects() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(new ArrayList<>());
        
        List<ProjectDTO> projects = Arrays.asList(
                createValidProject(),
                createValidProject()
        );
        projects.get(1).setName("Project 2");
        projects.get(1).setExternalId("project-2");

        // Act
        Map<String, Object> result = projectValidationService.validateProjectsWithDetails(projects);

        // Assert
        assertEquals(2, result.get("totalProjects"));
        assertEquals(2, result.get("validCount"));
        assertEquals(0, result.get("invalidCount"));
    }

    @Test
    @DisplayName("validateProjectsWithDetails debe detectar proyectos duplicados en archivo")
    void testValidateProjectsWithDetails_DuplicatedInFile() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(new ArrayList<>());
        
        ProjectDTO project1 = createValidProject();
        ProjectDTO project2 = createValidProject(); // Same externalId
        
        List<ProjectDTO> projects = Arrays.asList(project1, project2);

        // Act
        Map<String, Object> result = projectValidationService.validateProjectsWithDetails(projects);

        // Assert
        assertEquals(2, result.get("totalProjects"));
        assertEquals(1, result.get("validCount"));
        assertEquals(1, result.get("invalidCount"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> invalidProjects = (List<Map<String, Object>>) result.get("invalidProjects");
        assertTrue(invalidProjects.get(0).get("errors").toString().contains("duplicated"));
    }

    @Test
    @DisplayName("validateProjectsWithDetails debe detectar proyectos que ya existen en BD")
    void testValidateProjectsWithDetails_AlreadyExists() {
        // Arrange
        ProjectDTO existingProject = createValidProject();
        when(ldService.getAllProjects()).thenReturn(Arrays.asList(existingProject));
        
        ProjectDTO newProject = createValidProject(); // Same externalId
        List<ProjectDTO> projects = Arrays.asList(newProject);

        // Act
        Map<String, Object> result = projectValidationService.validateProjectsWithDetails(projects);

        // Assert
        assertEquals(1, result.get("totalProjects"));
        assertEquals(0, result.get("validCount"));
        assertEquals(1, result.get("invalidCount"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> invalidProjects = (List<Map<String, Object>>) result.get("invalidProjects");
        assertTrue(invalidProjects.get(0).get("errors").toString().contains("already exists"));
    }

    @Test
    @DisplayName("validateStudent debe validar un estudiante con las credenciales de la asignatura del proyecto")
    void testValidateStudent_ValidStudent() {
        // Arrange
        StudentDTO student = createStudent("Test Student", "github1", "taiga1");
        String githubUrl = "https://github.com/testorg";
        String taigaUrl = "https://tree.taiga.io/project/testuser-testproject/";

        // Act
        ValidationResult result = projectValidationService.validateStudent(
                1L, githubUrl, taigaUrl, student);

        // Assert
        assertTrue(result.isValid());
        verify(githubValidationService).validateUsersInOrganization(
                eq("testorg"),
                argThat(list -> list.contains("github1")),
                eq(GITHUB_TOKEN)
        );
        verify(taigaValidationService).validateUsersInProject(
                eq("testuser-testproject"),
                argThat(list -> list.contains("taiga1")),
                eq(TAIGA)
        );
    }

    @Test
    @DisplayName("validateStudent debe retornar error si estudiante es null")
    void testValidateStudent_NullStudent() {
        // Act
        ValidationResult result = projectValidationService.validateStudent(
                1L, "url", "url", null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateStudent debe retornar error si no tiene identidades")
    void testValidateStudent_NoIdentities() {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setName("Test");
        student.setIdentities(null);

        // Act
        ValidationResult result = projectValidationService.validateStudent(
                1L, "url", "url", student);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("has no defined identities"));
    }

    @Test
    @DisplayName("validateStudent debe manejar URLs nulas")
    void testValidateStudent_NullURLs() {
        // Arrange
        StudentDTO student = createStudent("Test", "github1", "taiga1");

        // Act
        ValidationResult result = projectValidationService.validateStudent(
                1L, null, null, student);

        // Assert
        assertTrue(result.isValid());
        verify(githubValidationService, never()).validateUsersInOrganization(anyString(), anyList(), anyString());
        verify(taigaValidationService, never()).validateUsersInProject(anyString(), anyList(), any());
    }

    @Test
    @DisplayName("validateStudent debe retornar error si el proyecto no existe")
    void testValidateStudent_ProjectNotFound() {
        // Arrange
        StudentDTO student = createStudent("Test", "github1", "taiga1");
        when(ldService.getProjectById(99L)).thenReturn(null);

        // Act
        ValidationResult unknownProject = projectValidationService.validateStudent(
                99L, "https://github.com/testorg", null, student);
        ValidationResult noProject = projectValidationService.validateStudent(
                null, "https://github.com/testorg", null, student);

        // Assert
        for (ValidationResult result : Arrays.asList(unknownProject, noProject)) {
            assertFalse(result.isValid());
            assertTrue(result.getErrors().get(0).contains("project of the student was not found"));
        }
        verifyNoInteractions(githubValidationService, taigaValidationService, subjectCredentialsService);
    }

    @Test
    @DisplayName("validateStudent debe retornar error si la asignatura no tiene credenciales")
    void testValidateStudent_CredentialsNotResolved() {
        // Arrange
        StudentDTO student = createStudent("Test", "github1", "taiga1");
        when(subjectCredentialsService.resolveBySubject(SUBJECT))
                .thenThrow(new CredentialsConfigException("Subject 'AMEP' has no github_token."));

        // Act
        ValidationResult result = projectValidationService.validateStudent(
                1L, "https://github.com/testorg", "https://tree.taiga.io/project/slug/", student);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("Subject 'AMEP' has no github_token."));
        verifyNoInteractions(githubValidationService, taigaValidationService);
    }

    @Test
    @DisplayName("validateProjectsWithDetails debe manejar excepción al cargar proyectos existentes")
    void testValidateProjectsWithDetails_ExceptionLoadingExisting() {
        // Arrange
        when(ldService.getAllProjects()).thenThrow(new RuntimeException("Database error"));
        
        List<ProjectDTO> projects = Arrays.asList(createValidProject());

        // Act
        Map<String, Object> result = projectValidationService.validateProjectsWithDetails(projects);

        // Assert
        assertEquals(1, result.get("totalProjects"));
        assertEquals(1, result.get("validCount")); // Should continue with validation
    }

    @Test
    @DisplayName("validateProject debe usar el token GitHub y el servidor Taiga de la asignatura de cada proyecto")
    void testValidateProject_EachSubjectUsesItsOwnCredentials() {
        // Arrange
        ProjectDTO amepProject = createValidProject();
        amepProject.setStudents(Arrays.asList(createStudent("Student 1", "github1", "taiga1")));

        ProjectDTO aswProject = createValidProject();
        aswProject.setExternalId("asw-team");
        aswProject.setSubject(OTHER_SUBJECT);
        aswProject.setStudents(Arrays.asList(createStudent("Student 2", "github2", "taiga2")));

        // Act
        assertTrue(projectValidationService.validateProject(amepProject).isValid());
        assertTrue(projectValidationService.validateProject(aswProject).isValid());

        // Assert
        verify(githubValidationService).validateOrganization("testorg", GITHUB_TOKEN);
        verify(githubValidationService).validateOrganization("testorg", "other-github-token");
        verify(githubValidationService).validateUsersInOrganization(eq("testorg"), anyList(), eq(GITHUB_TOKEN));
        verify(githubValidationService).validateUsersInOrganization(eq("testorg"), anyList(),
                eq("other-github-token"));
        verify(taigaValidationService).validateProjectBySlug("testuser-testproject", TAIGA);
        verify(taigaValidationService).validateProjectBySlug("testuser-testproject", OTHER_TAIGA);
        verify(taigaValidationService).validateUsersInProject(eq("testuser-testproject"), anyList(), eq(TAIGA));
        verify(taigaValidationService).validateUsersInProject(eq("testuser-testproject"), anyList(),
                eq(OTHER_TAIGA));
    }

    @Test
    @DisplayName("validateProject debe retornar error si la asignatura no tiene credenciales")
    void testValidateProject_CredentialsNotResolved() {
        // Arrange
        ProjectDTO project = createValidProject();
        project.setSubject("Unknown Subject");
        when(subjectCredentialsService.resolveBySubject("Unknown Subject")).thenThrow(
                new CredentialsConfigException("Subject 'Unknown Subject' is not defined in the credentials config."));

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Test Project"));
        assertTrue(result.getErrors().get(0).contains("'Unknown Subject' is not defined"));
        verifyNoInteractions(githubValidationService, taigaValidationService);
    }

    @Test
    @DisplayName("validateProject debe retornar error si el proyecto no tiene asignatura")
    void testValidateProject_NoSubject() {
        // Arrange
        ProjectDTO project = createValidProject();
        project.setSubject(null);
        when(subjectCredentialsService.resolveBySubject(null))
                .thenThrow(new CredentialsConfigException("The subject is empty, so its credentials cannot be resolved."));

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("The subject is empty"));
        verifyNoInteractions(githubValidationService, taigaValidationService);
    }

    @Test
    @DisplayName("validateProject debe retornar error si el equipo no está en 'teams' de su asignatura")
    void testValidateProject_TeamNotListedUnderSubject() {
        // Arrange: the team is listed under ASW, but the project says AMEP
        ProjectDTO project = createValidProject();
        project.setExternalId("asw-team");

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("team 'asw-team'"));
        assertTrue(result.getErrors().get(0).contains("is not listed in 'teams' of subject 'AMEP'"));
        verifyNoInteractions(githubValidationService, taigaValidationService);
    }

    @Test
    @DisplayName("validateProject debe retornar error si el proyecto no tiene externalId")
    void testValidateProject_NoExternalId() {
        // Arrange
        ProjectDTO project = createValidProject();
        project.setExternalId(null);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("is not listed in 'teams'"));
    }

    @Test
    @DisplayName("validateProjectsWithDetails debe rechazar solo los proyectos sin credenciales")
    void testValidateProjectsWithDetails_TeamNotListedIsInvalid() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(new ArrayList<>());

        ProjectDTO listed = createValidProject();
        ProjectDTO notListed = createValidProject();
        notListed.setName("Not Listed");
        notListed.setExternalId("not-listed-team");

        // Act
        Map<String, Object> result = projectValidationService
                .validateProjectsWithDetails(Arrays.asList(listed, notListed));

        // Assert
        assertEquals(1, result.get("validCount"));
        assertEquals(1, result.get("invalidCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> invalidProjects = (List<Map<String, Object>>) result.get("invalidProjects");
        assertSame(notListed, invalidProjects.get(0).get("project"));
        assertTrue(invalidProjects.get(0).get("errors").toString().contains("not-listed-team"));
    }

    @Test
    @DisplayName("validateProject debe manejar estudiantes sin identidades GitHub")
    void testValidateProject_StudentsWithoutGitHub() {
        // Arrange
        ProjectDTO project = createValidProject();
        List<StudentDTO> students = Arrays.asList(
                createStudent("Student 1", null, "taiga1")
        );
        project.setStudents(students);

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertTrue(result.isValid());
        verify(githubValidationService, never()).validateUsersInOrganization(
                anyString(),
                argThat(list -> list.isEmpty()),
                anyString()
        );
    }
}
