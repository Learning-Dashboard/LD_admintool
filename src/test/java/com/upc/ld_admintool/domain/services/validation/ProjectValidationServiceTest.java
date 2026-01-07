package com.upc.ld_admintool.domain.services.validation;

import com.upc.ld_admintool.domain.services.LDService;
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

    @InjectMocks
    private ProjectValidationService projectValidationService;

    private ProjectDTO createValidProject() {
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setName("Test Project");
        project.setExternalId("test-project");
        project.setGithubToken("github-token");

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

    @BeforeEach
    void setUp() {
        lenient().when(githubValidationService.validateOrganization(anyString(), anyString()))
                .thenReturn(new ValidationResult(true));
        lenient().when(githubValidationService.validateUsersInOrganization(anyString(), anyList(), anyString()))
                .thenReturn(new ValidationResult(true));
        lenient().when(taigaValidationService.validateProjectBySlug(anyString()))
                .thenReturn(new ValidationResult(true));
        lenient().when(taigaValidationService.validateUsersInProject(anyString(), anyList()))
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
        verify(githubValidationService).validateOrganization("testorg", "github-token");
        verify(taigaValidationService).validateProjectBySlug("testuser-testproject");
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
        ProjectDTO project = new ProjectDTO();
        project.setName("Test Project");
        
        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        ProjectIdentityDTO taigaIdentity = new ProjectIdentityDTO();
        taigaIdentity.setUrl("https://tree.taiga.io/project/test/");
        identities.put(DataSource.TAIGA, taigaIdentity);
        
        project.setIdentities(identities);

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
        ProjectDTO project = new ProjectDTO();
        project.setName("Test Project");
        
        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        ProjectIdentityDTO githubIdentity = new ProjectIdentityDTO();
        githubIdentity.setUrl("https://github.com/testorg");
        identities.put(DataSource.GITHUB, githubIdentity);
        
        project.setIdentities(identities);

        // Mock explícito para este test
        when(githubValidationService.validateOrganization("testorg", null))
                .thenReturn(new ValidationResult(true));

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
                eq("github-token")
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
                argThat(list -> list.size() == 2 && list.contains("taiga1") && list.contains("taiga2"))
        );
    }

    @Test
    @DisplayName("validateProject debe manejar URL de GitHub inválida")
    void testValidateProject_InvalidGitHubURL() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setName("Test Project");
        
        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        ProjectIdentityDTO githubIdentity = new ProjectIdentityDTO();
        githubIdentity.setUrl("invalid-url");
        identities.put(DataSource.GITHUB, githubIdentity);
        
        project.setIdentities(identities);

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
        ProjectDTO project = new ProjectDTO();
        project.setName("Test Project");
        
        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        ProjectIdentityDTO taigaIdentity = new ProjectIdentityDTO();
        taigaIdentity.setUrl("invalid-taiga-url");
        identities.put(DataSource.TAIGA, taigaIdentity);
        
        project.setIdentities(identities);

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
        
        when(taigaValidationService.validateProjectBySlug(anyString()))
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
    @DisplayName("validateStudent debe validar un estudiante correctamente")
    void testValidateStudent_ValidStudent() {
        // Arrange
        StudentDTO student = createStudent("Test Student", "github1", "taiga1");
        String githubUrl = "https://github.com/testorg";
        String taigaUrl = "https://tree.taiga.io/project/testuser-testproject/";

        // Act
        ValidationResult result = projectValidationService.validateStudent(
                githubUrl, taigaUrl, "token", student);

        // Assert
        assertTrue(result.isValid());
        verify(githubValidationService).validateUsersInOrganization(
                eq("testorg"),
                argThat(list -> list.contains("github1")),
                eq("token")
        );
        verify(taigaValidationService).validateUsersInProject(
                eq("testuser-testproject"),
                argThat(list -> list.contains("taiga1"))
        );
    }

    @Test
    @DisplayName("validateStudent debe retornar error si estudiante es null")
    void testValidateStudent_NullStudent() {
        // Act
        ValidationResult result = projectValidationService.validateStudent(
                "url", "url", "token", null);

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
                "url", "url", "token", student);

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
                null, null, "token", student);

        // Assert
        assertTrue(result.isValid());
        verify(githubValidationService, never()).validateUsersInOrganization(anyString(), anyList(), anyString());
        verify(taigaValidationService, never()).validateUsersInProject(anyString(), anyList());
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
    @DisplayName("validateProject debe manejar proyecto sin token GitHub")
    void testValidateProject_NoGitHubToken() {
        // Arrange
        ProjectDTO project = createValidProject();
        project.setGithubToken(null);

        // Mock explícito para este test
        when(githubValidationService.validateOrganization("testorg", null))
                .thenReturn(new ValidationResult(true));
        when(taigaValidationService.validateProjectBySlug("testuser-testproject"))
                .thenReturn(new ValidationResult(true));

        // Act
        ValidationResult result = projectValidationService.validateProject(project);

        // Assert
        assertTrue(result.isValid());
        verify(githubValidationService).validateOrganization("testorg", null);
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
