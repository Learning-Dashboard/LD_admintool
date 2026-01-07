package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.domain.services.exceptions.SaveSyncException;
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
 * Tests unitarios para ProjectService
 * Valida la lógica de gestión de proyectos y estudiantes
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService - Tests Unitarios")
class ProjectServiceTest {

    @Mock
    private LDService ldService;

    @Mock
    private LDEvalService ldEvalService;

    @InjectMocks
    private ProjectService projectService;

    private ProjectDTO testProject;
    private StudentDTO testStudent1;
    private StudentDTO testStudent2;
    private List<Map<String, Object>> metricCategories;
    private List<Map<String, Object>> factorCategories;

    @BeforeEach
    void setUp() {
        // Preparar proyecto de prueba
        testProject = new ProjectDTO();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setDescription("Test Description");
        testProject.setExternalId("test-external-id");

        // Preparar estudiantes de prueba
        testStudent1 = new StudentDTO();
        testStudent1.setId(1L);
        testStudent1.setName("Student One");

        testStudent2 = new StudentDTO();
        testStudent2.setId(2L);
        testStudent2.setName("Student Two");

        testProject.setStudents(Arrays.asList(testStudent1, testStudent2));
        
        // Preparar categorías de métricas
        metricCategories = new ArrayList<>();
        Map<String, Object> metricCat1 = new HashMap<>();
        metricCat1.put("name", "2 members - Quality");
        metricCat1.put("patternGroup", "quality");
        metricCategories.add(metricCat1);
        
        Map<String, Object> metricCat2 = new HashMap<>();
        metricCat2.put("name", "3 members - Quality");
        metricCat2.put("patternGroup", "quality");
        metricCategories.add(metricCat2);
        
        // Preparar categorías de factores
        factorCategories = new ArrayList<>();
        Map<String, Object> factorCat1 = new HashMap<>();
        factorCat1.put("name", "2 members - Performance");
        factorCat1.put("patternGroup", "performance");
        factorCategories.add(factorCat1);
        
        Map<String, Object> factorCat2 = new HashMap<>();
        factorCat2.put("name", "3 members - Performance");
        factorCat2.put("patternGroup", "performance");
        factorCategories.add(factorCat2);
    }

    @Test
    @DisplayName("Debe listar proyectos con estudiantes correctamente")
    void testLlistarProjectesAmbStudents_Success() {
        // Arrange
        List<ProjectDTO> rawProjects = Arrays.asList(testProject);
        when(ldService.getAllProjects()).thenReturn(rawProjects);
        when(ldService.getProjectById(1L)).thenReturn(testProject);

        // Act
        List<ProjectDTO> result = projectService.llistarProjectesAmbStudents();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Project", result.get(0).getName());
        verify(ldService, times(1)).getAllProjects();
        verify(ldService, times(1)).getProjectById(1L);
    }

    @Test
    @DisplayName("Debe obtener proyecto por ID correctamente")
    void testGetProjectById_Success() {
        // Arrange
        when(ldService.getProjectById(1L)).thenReturn(testProject);

        // Act
        ProjectDTO result = projectService.getProjectById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Project", result.getName());
        verify(ldService, times(1)).getProjectById(1L);
    }

    @Test
    @DisplayName("Debe importar proyectos con estudiantes y triggerar refresh")
    void testImportProjects_WithStudents_Success() {
        // Arrange
        List<ProjectDTO> projects = Arrays.asList(testProject);
        when(ldService.createProject(any(ProjectDTO.class))).thenReturn(1L);
        doNothing().when(ldService).createStudent(anyLong(), any(StudentDTO.class));
        when(ldEvalService.triggerRefresh()).thenReturn(true);

        // Act
        projectService.importProjects(projects);

        // Assert
        verify(ldService, times(1)).createProject(testProject);
        verify(ldService, times(2)).createStudent(anyLong(), any(StudentDTO.class));
        verify(ldEvalService, times(1)).triggerRefresh();
    }

    @Test
    @DisplayName("Debe manejar proyectos sin estudiantes")
    void testImportProjects_WithoutStudents_Success() {
        // Arrange
        ProjectDTO projectWithoutStudents = new ProjectDTO();
        projectWithoutStudents.setName("Project Without Students");
        projectWithoutStudents.setStudents(null);
        
        List<ProjectDTO> projects = Arrays.asList(projectWithoutStudents);
        when(ldService.createProject(any(ProjectDTO.class))).thenReturn(1L);
        when(ldEvalService.triggerRefresh()).thenReturn(true);

        // Act
        projectService.importProjects(projects);

        // Assert
        verify(ldService, times(1)).createProject(projectWithoutStudents);
        verify(ldService, never()).createStudent(anyLong(), any(StudentDTO.class));
        verify(ldEvalService, times(1)).triggerRefresh();
    }

    @Test
    @DisplayName("No debe triggerar refresh si no se crea ningún proyecto")
    void testImportProjects_NoProjectsCreated_NoRefresh() {
        // Arrange
        List<ProjectDTO> projects = Arrays.asList(testProject);
        when(ldService.createProject(any(ProjectDTO.class))).thenReturn(null);

        // Act
        projectService.importProjects(projects);

        // Assert
        verify(ldService, times(1)).createProject(testProject);
        verify(ldEvalService, never()).triggerRefresh();
    }

    @Test
    @DisplayName("Debe retornar null cuando el proyecto no existe")
    void testGetProjectById_NotFound() {
        // Arrange
        when(ldService.getProjectById(999L)).thenReturn(null);

        // Act
        ProjectDTO result = projectService.getProjectById(999L);

        // Assert
        assertNull(result);
        verify(ldService, times(1)).getProjectById(999L);
    }

    @Test
    @DisplayName("Debe manejar lista vacía de proyectos")
    void testLlistarProjectesAmbStudents_EmptyList() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(Arrays.asList());

        // Act
        List<ProjectDTO> result = projectService.llistarProjectesAmbStudents();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ldService, times(1)).getAllProjects();
        verify(ldService, never()).getProjectById(anyLong());
    }

    @Test
    @DisplayName("Debe manejar cuando getProjectById retorna null durante listado")
    void testLlistarProjectesAmbStudents_NullProject() {
        // Arrange
        List<ProjectDTO> rawProjects = Arrays.asList(testProject);
        when(ldService.getAllProjects()).thenReturn(rawProjects);
        when(ldService.getProjectById(1L)).thenReturn(null);

        // Act
        List<ProjectDTO> result = projectService.llistarProjectesAmbStudents();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testProject, result.get(0)); // Should use original project
    }

    @Test
    @DisplayName("Debe manejar múltiples proyectos en importación")
    void testImportProjects_MultipleProjects() {
        // Arrange
        ProjectDTO project2 = new ProjectDTO();
        project2.setName("Project 2");
        project2.setStudents(null);
        
        List<ProjectDTO> projects = Arrays.asList(testProject, project2);
        when(ldService.createProject(any(ProjectDTO.class)))
                .thenReturn(1L)
                .thenReturn(2L);
        when(ldEvalService.triggerRefresh()).thenReturn(true);

        // Act
        projectService.importProjects(projects);

        // Assert
        verify(ldService, times(2)).createProject(any(ProjectDTO.class));
        verify(ldEvalService, times(1)).triggerRefresh();
    }

    @Test
    @DisplayName("Debe manejar lista vacía en importación")
    void testImportProjects_EmptyList() {
        // Arrange
        List<ProjectDTO> emptyList = Arrays.asList();

        // Act
        projectService.importProjects(emptyList);

        // Assert
        verify(ldService, never()).createProject(any(ProjectDTO.class));
        verify(ldEvalService, never()).triggerRefresh();
    }

    @Test
    @DisplayName("Debe deletear proyecto correctamente")
    void testEsborrarProjecte_Success() {
        // Arrange
        doNothing().when(ldService).deleteProject(1L);
        when(ldEvalService.triggerRefresh()).thenReturn(true);

        // Act
        projectService.esborrarProjecte(1L);

        // Assert
        verify(ldService, times(1)).deleteProject(1L);
        verify(ldEvalService, times(1)).triggerRefresh();
    }

    @Test
    @DisplayName("Debe manejar error al obtener proyecto por ID")
    void testGetProjectById_Exception() {
        // Arrange
        when(ldService.getProjectById(1L)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            projectService.getProjectById(1L);
        });
    }

    @Test
    @DisplayName("Debe continuar importando proyectos si uno falla")
    void testImportProjects_PartialFailure() {
        // Arrange
        ProjectDTO project2 = new ProjectDTO();
        project2.setName("Project 2");
        
        List<ProjectDTO> projects = Arrays.asList(testProject, project2);
        when(ldService.createProject(testProject)).thenReturn(null); // Falla el primero
        when(ldService.createProject(project2)).thenReturn(2L); // Éxito el segundo
        when(ldEvalService.triggerRefresh()).thenReturn(true);

        // Act
        projectService.importProjects(projects);

        // Assert
        verify(ldService, times(2)).createProject(any(ProjectDTO.class));
        verify(ldEvalService, times(1)).triggerRefresh(); // Se llama porque al menos uno tuvo éxito
    }

    @Test
    @DisplayName("modificarProjecte debe lanzar excepción si proyecto no existe")
    void testModificarProjecte_ProjectNotFound() {
        // Arrange
        when(ldService.getProjectById(999L)).thenReturn(null);
        ProjectDTO updatedProject = new ProjectDTO();

        // Act & Assert
        assertThrows(SaveSyncException.class, () -> {
            projectService.modificarProjecte(999L, updatedProject);
        });
    }

    @Test
    @DisplayName("modificarProjecte debe lanzar excepción cuando categorías no están disponibles")
    void testModificarProjecte_CategoryValidationFails() {
        // Arrange
        ProjectDTO original = new ProjectDTO();
        original.setId(1L);
        original.setExternalId("test-ext");
        original.setStudents(Arrays.asList());
        
        ProjectDTO updated = new ProjectDTO();
        updated.setExternalId("test-ext");
        updated.setStudents(Arrays.asList(testStudent1));
        
        when(ldService.getProjectById(1L)).thenReturn(original);
        when(ldService.getAllMetricsCategories()).thenReturn(new ArrayList<>());
        when(ldService.getAllFactorsCategories()).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(SaveSyncException.class, () -> {
            projectService.modificarProjecte(1L, updated);
        });
    }

    @Test
    @DisplayName("modificarProjecte debe eliminar estudiantes")
    void testModificarProjecte_RemoveStudents() {
        // Arrange
        StudentDTO existingStudent = new StudentDTO();
        existingStudent.setId(10L);
        existingStudent.setName("Existing");
        
        ProjectDTO original = new ProjectDTO();
        original.setId(1L);
        original.setExternalId("test-ext");
        original.setStudents(Arrays.asList(existingStudent));
        
        ProjectDTO updated = new ProjectDTO();
        updated.setExternalId("test-ext");
        updated.setStudents(Arrays.asList());
        
        when(ldService.getProjectById(1L)).thenReturn(original);
        when(ldService.getAllMetricsCategories()).thenReturn(new ArrayList<>());
        when(ldService.getAllFactorsCategories()).thenReturn(new ArrayList<>());

        // Act & Assert - debería fallar por falta de categorías
        assertThrows(SaveSyncException.class, () -> {
            projectService.modificarProjecte(1L, updated);
        });
    }

    @Test
    @DisplayName("validateCategoriesForNewTeamSize debe lanzar excepción si categoría no existe")
    void testValidateCategoriesForNewTeamSize_CategoryNotFound() {
        // Arrange
        MetricDTO metric = new MetricDTO();
        metric.setId("1");
        metric.setCategoryName("2 members - Quality");
        
        Map<String, Object> category = new HashMap<>();
        category.put("name", "2 members - Quality");
        category.put("patternGroup", "quality-pattern");
        
        List<Map<String, Object>> categories = Arrays.asList(category);
        
        lenient().when(ldService.getProjectById(1L)).thenReturn(testProject);
        lenient().when(ldService.getAllMetricsCategories()).thenReturn(categories);
        lenient().when(ldService.getAllFactorsCategories()).thenReturn(new ArrayList<>());
        lenient().when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList(metric));
        lenient().when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList());

        // Act & Assert - Pedir 5 miembros cuando solo hay categorías para 2
        assertThrows(RuntimeException.class, () -> {
            projectService.validateCategoriesForNewTeamSize(1L, 5);
        });
    }

    @Test
    @DisplayName("validateCategoriesForNewTeamSize debe pasar si categoría existe")
    void testValidateCategoriesForNewTeamSize_Success() {
        // Arrange
        MetricDTO metric = new MetricDTO();
        metric.setId("1");
        metric.setCategoryName("2 members - Quality");
        
        when(ldService.getProjectById(1L)).thenReturn(testProject);
        when(ldService.getAllMetricsCategories()).thenReturn(metricCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(factorCategories);
        when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList(metric));
        when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList());

        // Act & Assert - Pedir 3 miembros (existe la categoría)
        assertDoesNotThrow(() -> {
            projectService.validateCategoriesForNewTeamSize(1L, 3);
        });
    }

    @Test
    @DisplayName("updateCategoriesForProject debe actualizar métricas y factores")
    void testUpdateCategoriesForProject_Success() {
        // Arrange
        MetricDTO metric = new MetricDTO();
        metric.setId("1");
        metric.setExternalId("metric_test");
        metric.setCategoryName("2 members - Quality");
        metric.setScope("project");
        
        FactorDTO factor = new FactorDTO();
        factor.setId("1");
        factor.setExternalId("factor_test");
        factor.setCategory("2 members - Performance");
        
        when(ldService.getProjectById(1L)).thenReturn(testProject);
        when(ldService.getAllMetricsCategories()).thenReturn(metricCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(factorCategories);
        when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList(metric));
        when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList(factor));

        // Act
        projectService.updateCategoriesForProject(1L);

        // Assert
        verify(ldService).getMetricsByProject(testProject.getExternalId());
        verify(ldService).getFactorsByProject(testProject.getExternalId());
    }

    @Test
    @DisplayName("synchronizeCategoriesAfterDataImport debe sincronizar categorías entre proyectos")
    void testSynchronizeCategoriesAfterDataImport_Success() {
        // Arrange
        ProjectDTO project1 = new ProjectDTO();
        project1.setId(1L);
        project1.setExternalId("proj1");
        project1.setSubject("Math");
        
        ProjectDTO project2 = new ProjectDTO();
        project2.setId(2L);
        project2.setExternalId("proj2");
        project2.setSubject("Math");
        
        when(ldService.getAllProjects()).thenReturn(Arrays.asList(project1, project2));
        when(ldService.getProjectById(1L)).thenReturn(project1);
        when(ldService.getProjectById(2L)).thenReturn(project2);
        when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList());
        when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList());
        when(ldEvalService.triggerRefresh()).thenReturn(true);

        // Act
        projectService.synchronizeCategoriesAfterDataImport();

        // Assert
        verify(ldEvalService).triggerRefresh();
    }

    @Test
    @DisplayName("synchronizeCategoriesAfterDataImport debe manejar excepciones")
    void testSynchronizeCategoriesAfterDataImport_Exception() {
        // Arrange
        when(ldService.getAllProjects()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert - No debe lanzar excepción, solo logear
        assertDoesNotThrow(() -> {
            projectService.synchronizeCategoriesAfterDataImport();
        });
    }

    @Test
    @DisplayName("esborrarProjecte debe deletear proyecto y triggerar refresh en LDEval")
    void testEsborrarProjecte_SuccessWithRefresh() {
        // Arrange
        doNothing().when(ldService).deleteProject(1L);
        when(ldEvalService.triggerRefresh()).thenReturn(true);

        // Act
        projectService.esborrarProjecte(1L);

        // Assert
        verify(ldService, times(1)).deleteProject(1L);
        verify(ldEvalService, times(1)).triggerRefresh();
    }

    @Test
    @DisplayName("updateCategoriesForProject debe lanzar excepción si proyecto no existe")
    void testUpdateCategoriesForProject_ProjectNotFound() {
        // Arrange
        when(ldService.getProjectById(999L)).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            projectService.updateCategoriesForProject(999L);
        });
    }

    @Test
    @DisplayName("updateCategoriesForProject con override debe usar valores proporcionados")
    void testUpdateCategoriesForProject_WithOverride() {
        // Arrange
        ProjectDTO overrideProject = new ProjectDTO();
        overrideProject.setExternalId("override-ext");
        overrideProject.setStudents(Arrays.asList(testStudent1, testStudent2, new StudentDTO()));
        
        when(ldService.getAllMetricsCategories()).thenReturn(metricCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(factorCategories);
        when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList());
        when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList());

        // Act
        projectService.updateCategoriesForProject(1L, overrideProject, 3);

        // Assert
        verify(ldService).getMetricsByProject("override-ext");
        verify(ldService, never()).getProjectById(1L); // No debe buscar porque se proporciona override
    }

    @Test
    @DisplayName("validateCategoriesForNewTeamSize debe retornar si proyecto no existe")
    void testValidateCategoriesForNewTeamSize_ProjectNotFound() {
        // Arrange
        when(ldService.getProjectById(999L)).thenReturn(null);

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> {
            projectService.validateCategoriesForNewTeamSize(999L, 2);
        });
    }

    @Test
    @DisplayName("validateCategoriesForNewTeamSize debe validar factores correctamente")
    void testValidateCategoriesForNewTeamSize_FactorsValidation() {
        // Arrange
        FactorDTO factor = new FactorDTO();
        factor.setId("1");
        factor.setCategory("2 members - Performance");
        
        when(ldService.getProjectById(1L)).thenReturn(testProject);
        when(ldService.getAllMetricsCategories()).thenReturn(metricCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(factorCategories);
        when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList());
        when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList(factor));

        // Act & Assert
        assertDoesNotThrow(() -> {
            projectService.validateCategoriesForNewTeamSize(1L, 3);
        });
    }

    @Test
    @DisplayName("validateCategoriesForNewTeamSize debe lanzar excepción para factores sin categoría")
    void testValidateCategoriesForNewTeamSize_FactorCategoryNotFound() {
        // Arrange
        FactorDTO factor = new FactorDTO();
        factor.setId("1");
        factor.setCategory("2 members - Performance");
        
        when(ldService.getProjectById(1L)).thenReturn(testProject);
        when(ldService.getAllMetricsCategories()).thenReturn(metricCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(factorCategories);
        when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList());
        when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList(factor));

        // Act & Assert - Pedir 5 miembros cuando solo hay categorías para 2 y 3
        assertThrows(RuntimeException.class, () -> {
            projectService.validateCategoriesForNewTeamSize(1L, 5);
        });
    }

    @Test
    @DisplayName("modificarProjecte debe manejar estudiantes sin cambios")
    void testModificarProjecte_NoStudentChanges() {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setId(10L);
        student.setName("Student");
        
        ProjectDTO original = new ProjectDTO();
        original.setId(1L);
        original.setExternalId("test-ext");
        original.setStudents(Arrays.asList(student));
        
        ProjectDTO updated = new ProjectDTO();
        updated.setExternalId("test-ext");
        updated.setStudents(Arrays.asList(student)); // Mismo estudiante
        
        when(ldService.getProjectById(1L)).thenReturn(original);
        when(ldService.getAllMetricsCategories()).thenReturn(metricCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(factorCategories);
        when(ldEvalService.triggerRefresh()).thenReturn(true);
        when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList());
        when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList());
        doNothing().when(ldService).importMetrics();
        doNothing().when(ldService).importQualityFactors();
        doNothing().when(ldService).fetchStrategicIndicators();

        // Act
        SaveSyncResponseDTO result = projectService.modificarProjecte(1L, updated);

        // Assert
        assertNotNull(result);
        verify(ldService, never()).createStudent(anyLong(), any());
        verify(ldService, never()).deleteStudent(anyLong());
    }

    @Test
    @DisplayName("modificarProjecte debe manejar fallo en LDEval refresh")
    void testModificarProjecte_LDEvalRefreshFails() {
        // Arrange
        ProjectDTO original = new ProjectDTO();
        original.setId(1L);
        original.setExternalId("test-ext");
        original.setStudents(Arrays.asList());
        
        ProjectDTO updated = new ProjectDTO();
        updated.setExternalId("test-ext");
        updated.setStudents(Arrays.asList());
        
        when(ldService.getProjectById(1L)).thenReturn(original);
        when(ldService.getAllMetricsCategories()).thenReturn(metricCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(factorCategories);
        when(ldEvalService.triggerRefresh()).thenReturn(false); // Falla el refresh
        when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList());
        when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList());

        // Act & Assert
        assertThrows(SaveSyncException.class, () -> {
            projectService.modificarProjecte(1L, updated);
        });
    }

    @Test
    @DisplayName("synchronizeCategoriesAfterDataImport debe manejar proyectos sin subject")
    void testSynchronizeCategoriesAfterDataImport_NoSubject() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setExternalId("proj1");
        project.setSubject(null);
        project.setName(null);
        
        when(ldService.getAllProjects()).thenReturn(Arrays.asList(project));
        when(ldService.getProjectById(1L)).thenReturn(project);
        when(ldEvalService.triggerRefresh()).thenReturn(true);

        // Act
        projectService.synchronizeCategoriesAfterDataImport();

        // Assert
        verify(ldEvalService).triggerRefresh();
    }

    @Test
    @DisplayName("modificarProjecte debe manejar estudiantes null")
    void testModificarProjecte_NullStudents() {
        // Arrange
        ProjectDTO original = new ProjectDTO();
        original.setId(1L);
        original.setExternalId("test-ext");
        original.setStudents(null);
        
        ProjectDTO updated = new ProjectDTO();
        updated.setExternalId("test-ext");
        updated.setStudents(null);
        
        when(ldService.getProjectById(1L)).thenReturn(original);
        when(ldService.getAllMetricsCategories()).thenReturn(metricCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(factorCategories);
        when(ldEvalService.triggerRefresh()).thenReturn(true);
        when(ldService.getMetricsByProject(anyString())).thenReturn(Arrays.asList());
        when(ldService.getFactorsByProject(anyString())).thenReturn(Arrays.asList());
        doNothing().when(ldService).importMetrics();
        doNothing().when(ldService).importQualityFactors();
        doNothing().when(ldService).fetchStrategicIndicators();

        // Act
        SaveSyncResponseDTO result = projectService.modificarProjecte(1L, updated);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getFinalTeamSize());
    }
}

