package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.rest.DTO.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para LDService
 * Valida la comunicación con la API del Learning Dashboard
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LDService - Tests Unitarios")
class LDServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LDService ldService;

    private static final String LD_API_URL = "http://localhost:8888/api";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ldService, "ldApiUrl", LD_API_URL);
        ReflectionTestUtils.setField(ldService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("createProject debe crear proyecto y retornar ID")
    void testCreateProject_Success() {
        // Arrange
        ProjectDTO inputProject = new ProjectDTO();
        inputProject.setName("Test Project");
        
        ProjectDTO responseProject = new ProjectDTO();
        responseProject.setId(1L);
        responseProject.setName("Test Project");
        
        ResponseEntity<ProjectDTO> responseEntity = new ResponseEntity<>(responseProject, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(ProjectDTO.class)))
                .thenReturn(responseEntity);

        // Act
        Long projectId = ldService.createProject(inputProject);

        // Assert
        assertNotNull(projectId);
        assertEquals(1L, projectId);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(ProjectDTO.class));
    }

    @Test
    @DisplayName("createProject debe retornar null en caso de error HTTP")
    void testCreateProject_HttpError() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setName("Test Project");
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(ProjectDTO.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act
        Long projectId = ldService.createProject(project);

        // Assert
        assertNull(projectId);
    }

    @Test
    @DisplayName("getAllProjects debe retornar lista de proyectos")
    void testGetAllProjects_Success() {
        // Arrange
        ProjectDTO project1 = new ProjectDTO();
        project1.setId(1L);
        project1.setName("Project 1");
        
        ProjectDTO project2 = new ProjectDTO();
        project2.setId(2L);
        project2.setName("Project 2");
        
        ProjectDTO[] projects = {project1, project2};
        ResponseEntity<ProjectDTO[]> responseEntity = new ResponseEntity<>(projects, HttpStatus.OK);
        
        when(restTemplate.getForEntity(anyString(), eq(ProjectDTO[].class)))
                .thenReturn(responseEntity);

        // Act
        List<ProjectDTO> result = ldService.getAllProjects();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Project 1", result.get(0).getName());
        assertEquals("Project 2", result.get(1).getName());
    }

    @Test
    @DisplayName("getProjectById debe retornar proyecto por ID")
    void testGetProjectById_Success() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setName("Test Project");
        
        ResponseEntity<ProjectDTO> responseEntity = new ResponseEntity<>(project, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(ProjectDTO.class)))
                .thenReturn(responseEntity);

        // Act
        ProjectDTO result = ldService.getProjectById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Project", result.getName());
    }

    @Test
    @DisplayName("getProjectById debe retornar null si proyecto no existe")
    void testGetProjectById_NotFound() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(ProjectDTO.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act
        ProjectDTO result = ldService.getProjectById(999L);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("createStudent debe crear estudiante sin errores")
    void testCreateStudent_Success() {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setName("John Doe");
        
        ResponseEntity<StudentDTO> responseEntity = new ResponseEntity<>(student, HttpStatus.CREATED);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(StudentDTO.class)))
                .thenReturn(responseEntity);

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> ldService.createStudent(1L, student));
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), eq(StudentDTO.class));
    }

    @Test
    @DisplayName("createStudent debe manejar error HTTP silenciosamente")
    void testCreateStudent_HttpError() {
        // Arrange
        StudentDTO student = new StudentDTO();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(StudentDTO.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> ldService.createStudent(1L, student));
    }

    @Test
    @DisplayName("deleteStudent debe eliminar estudiante")
    void testDeleteStudent_Success() {
        // Arrange
        doNothing().when(restTemplate).delete(anyString());

        // Act & Assert
        assertDoesNotThrow(() -> ldService.deleteStudent(1L));
        verify(restTemplate, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("deleteStudent debe manejar error HTTP")
    void testDeleteStudent_HttpError() {
        // Arrange
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(restTemplate).delete(anyString());

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> ldService.deleteStudent(1L));
    }

    @Test
    @DisplayName("getMetricsByProject debe retornar lista de métricas")
    void testGetMetricsByProject_Success() {
        // Arrange
        Map<String, Object> metric1 = new HashMap<>();
        metric1.put("id", 1);
        metric1.put("externalId", "ext-1");
        metric1.put("name", "Metric 1");
        metric1.put("description", "Description 1");
        metric1.put("categoryName", "Category 1");
        metric1.put("scope", "project");
        
        List<Map<String, Object>> metricsData = Arrays.asList(metric1);
        ResponseEntity<List> responseEntity = new ResponseEntity<>(metricsData, HttpStatus.OK);
        
        when(restTemplate.getForEntity(anyString(), eq(List.class)))
                .thenReturn(responseEntity);

        // Act
        List<MetricDTO> result = ldService.getMetricsByProject("test-project");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Metric 1", result.get(0).getName());
    }

    @Test
    @DisplayName("getMetricsByProject debe retornar lista vacía en caso de error")
    void testGetMetricsByProject_HttpError() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(List.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        List<MetricDTO> result = ldService.getMetricsByProject("test-project");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllMetricsCategories debe retornar lista de categorías")
    void testGetAllMetricsCategories_Success() {
        // Arrange
        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, Object> category = new HashMap<>();
        category.put("name", "Performance");
        categories.add(category);
        
        ResponseEntity<List> responseEntity = new ResponseEntity<>(categories, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(List.class)))
                .thenReturn(responseEntity);

        // Act
        List<Map<String, Object>> result = ldService.getAllMetricsCategories();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getMetricsCategoriesList debe retornar lista de nombres")
    void testGetMetricsCategoriesList_Success() {
        // Arrange
        List<String> categoriesList = Arrays.asList("Category1", "Category2");
        ResponseEntity<List> responseEntity = new ResponseEntity<>(categoriesList, HttpStatus.OK);
        
        when(restTemplate.getForEntity(anyString(), eq(List.class)))
                .thenReturn(responseEntity);

        // Act
        List<String> result = ldService.getMetricsCategoriesList();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("importarCategoriesMetriques debe importar todas las categorías")
    void testImportarCategoriesMetriques_Success() {
        // Arrange
        CategoryDTO category1 = new CategoryDTO();
        category1.setCategory("Category1");
        
        CategoryDTO category2 = new CategoryDTO();
        category2.setCategory("Category2");
        category2.setPatternGroup("group1");
        
        List<CategoryDTO> categories = Arrays.asList(category1, category2);
        
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(responseEntity);

        // Act & Assert
        assertDoesNotThrow(() -> ldService.importarCategoriesMetriques(categories));
        verify(restTemplate, times(2)).postForEntity(anyString(), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    @DisplayName("importarCategoriesMetriques debe continuar si una categoría falla")
    void testImportarCategoriesMetriques_PartialFailure() {
        // Arrange
        CategoryDTO category1 = new CategoryDTO();
        category1.setCategory("Category1");
        
        CategoryDTO category2 = new CategoryDTO();
        category2.setCategory("Category2");
        
        List<CategoryDTO> categories = Arrays.asList(category1, category2);
        
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Error"))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> ldService.importarCategoriesMetriques(categories));
    }

    @Test
    @DisplayName("createStudent debe manejar error HTTP BadRequest")
    void testCreateStudent_HttpErrorBadRequest() {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setName("Test Student");
        
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
                .when(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(StudentDTO.class));

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> ldService.createStudent(1L, student));
    }

    @Test
    @DisplayName("deleteStudent debe manejar error HTTP NotFound")
    void testDeleteStudent_HttpErrorNotFound() {
        // Arrange
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .when(restTemplate).delete(anyString());

        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> ldService.deleteStudent(999L));
    }

    @Test
    @DisplayName("updateProject debe actualizar proyecto correctamente")
    void testUpdateProject_Success() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setName("Updated Project");
        
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(response);

        // Act & Assert
        assertDoesNotThrow(() -> ldService.updateProject(1L, project));
    }



    @Test
    @DisplayName("editMetric debe actualizar métrica")
    void testEditMetric_Success() {
        // Arrange
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(response);

        // Act & Assert
        assertDoesNotThrow(() -> ldService.editMetric(1L, "0.8", "http://url", "Coverage", "PROJECT", "project1"));
    }

    @Test
    @DisplayName("importarCategoriesFactors debe importar categorías")
    void testImportarCategoriesFactors_Success() {
        // Arrange
        CategoryDTO category = new CategoryDTO();
        category.setCategory("Quality");
        
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(response);

        // Act & Assert
        assertDoesNotThrow(() -> ldService.importarCategoriesFactors(Arrays.asList(category)));
    }



    @Test
    @DisplayName("importarCategoriesStrategicIndicators debe importar intervalos")
    void testImportarCategoriesStrategicIndicators_Success() {
        // Arrange
        IntervalDTO interval = new IntervalDTO();
        interval.setName("Good");
        interval.setColor("#00FF00");
        
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(response);

        // Act & Assert
        assertDoesNotThrow(() -> ldService.importarCategoriesStrategicIndicators(Arrays.asList(interval)));
    }







    @Test
    @DisplayName("updateFactorCategory debe actualizar categoría")
    void testUpdateFactorCategory_Success() {
        // Arrange
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(response);

        // Act & Assert
        assertDoesNotThrow(() -> ldService.updateFactorCategory(1L, "NewCategory", "project1"));
    }

    @Test
    @DisplayName("deleteProject debe eliminar proyecto")
    void testDeleteProject_Success() {
        // Arrange
        doNothing().when(restTemplate).delete(anyString());

        // Act & Assert
        assertDoesNotThrow(() -> ldService.deleteProject(1L));
        verify(restTemplate, times(1)).delete(anyString());
    }
}
