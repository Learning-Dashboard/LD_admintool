package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.rest.DTO.MetricDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para MetricsService
 * Valida la gestión de métricas y sus categorías
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsService - Tests Unitarios")
class MetricsServiceTest {

    @Mock
    private LDService ldService;

    @InjectMocks
    private MetricsService metricsService;

    private List<MetricDTO> testMetrics;
    private List<String> testCategoriesList;
    private List<Map<String, Object>> testCategoriesMap;

    @BeforeEach
    void setUp() {
        // Preparar métricas de prueba
        MetricDTO metric1 = new MetricDTO();
        metric1.setId("1");
        metric1.setName("Test Metric 1");
        
        MetricDTO metric2 = new MetricDTO();
        metric2.setId("2");
        metric2.setName("Test Metric 2");
        
        testMetrics = Arrays.asList(metric1, metric2);

        // Preparar categorías
        testCategoriesList = Arrays.asList("Performance", "Security", "Maintainability");

        // Preparar mapa de categorías
        Map<String, Object> categoryMap1 = new HashMap<>();
        categoryMap1.put("name", "Performance");
        categoryMap1.put("metrics", 10);
        
        Map<String, Object> categoryMap2 = new HashMap<>();
        categoryMap2.put("name", "Security");
        categoryMap2.put("metrics", 7);
        
        testCategoriesMap = Arrays.asList(categoryMap1, categoryMap2);
    }

    @Test
    @DisplayName("Debe obtener métricas por proyecto correctamente")
    void testGetMetricsByProject_Success() {
        // Arrange
        String projectId = "test-project-123";
        when(ldService.getMetricsByProject(projectId)).thenReturn(testMetrics);

        // Act
        List<MetricDTO> result = metricsService.getMetricsByProject(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Metric 1", result.get(0).getName());
        assertEquals("Test Metric 2", result.get(1).getName());
        verify(ldService, times(1)).getMetricsByProject(projectId);
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay métricas")
    void testGetMetricsByProject_EmptyList() {
        // Arrange
        String projectId = "empty-project";
        when(ldService.getMetricsByProject(projectId)).thenReturn(Arrays.asList());

        // Act
        List<MetricDTO> result = metricsService.getMetricsByProject(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ldService, times(1)).getMetricsByProject(projectId);
    }

    @Test
    @DisplayName("Debe obtener lista de categorías de métricas")
    void testGetMetricsCategoriesList_Success() {
        // Arrange
        when(ldService.getMetricsCategoriesList()).thenReturn(testCategoriesList);

        // Act
        List<String> result = metricsService.getMetricsCategoriesList();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("Performance"));
        assertTrue(result.contains("Security"));
        assertTrue(result.contains("Maintainability"));
        verify(ldService, times(1)).getMetricsCategoriesList();
    }

    @Test
    @DisplayName("Debe obtener todas las categorías de métricas con detalles")
    void testGetAllMetricsCategories_Success() {
        // Arrange
        when(ldService.getAllMetricsCategories()).thenReturn(testCategoriesMap);

        // Act
        List<Map<String, Object>> result = metricsService.getAllMetricsCategories();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Performance", result.get(0).get("name"));
        assertEquals(10, result.get(0).get("metrics"));
        verify(ldService, times(1)).getAllMetricsCategories();
    }

    @Test
    @DisplayName("Debe editar métrica correctamente con todos los parámetros")
    void testEditMetric_Success() {
        // Arrange
        Long metricId = 1L;
        String threshold = "0.7";
        String url = "http://example.com/metric";
        String categoryName = "Performance";
        String scope = "global";
        String project = "project-123";
        
        doNothing().when(ldService).editMetric(metricId, threshold, url, categoryName, scope, project);

        // Act
        metricsService.editMetric(metricId, threshold, url, categoryName, scope, project);

        // Assert
        verify(ldService, times(1)).editMetric(metricId, threshold, url, categoryName, scope, project);
        verifyNoMoreInteractions(ldService);
    }

    @Test
    @DisplayName("Debe importar métricas correctamente")
    void testImportMetrics_Success() {
        // Arrange
        doNothing().when(ldService).importMetrics();

        // Act
        metricsService.importMetrics();

        // Assert
        verify(ldService, times(1)).importMetrics();
        verifyNoMoreInteractions(ldService);
    }

    @Test
    @DisplayName("Debe manejar valores null en editMetric")
    void testEditMetric_WithNullValues() {
        // Arrange
        Long metricId = 1L;
        doNothing().when(ldService).editMetric(metricId, null, null, null, null, null);

        // Act
        metricsService.editMetric(metricId, null, null, null, null, null);

        // Assert
        verify(ldService, times(1)).editMetric(metricId, null, null, null, null, null);
    }

    @Test
    @DisplayName("Debe delegar correctamente al LDService")
    void testServiceDelegation() {
        // Arrange
        String projectId = "test-project";
        when(ldService.getMetricsByProject(projectId)).thenReturn(testMetrics);
        when(ldService.getMetricsCategoriesList()).thenReturn(testCategoriesList);
        when(ldService.getAllMetricsCategories()).thenReturn(testCategoriesMap);

        // Act
        metricsService.getMetricsByProject(projectId);
        metricsService.getMetricsCategoriesList();
        metricsService.getAllMetricsCategories();

        // Assert
        verify(ldService, times(1)).getMetricsByProject(projectId);
        verify(ldService, times(1)).getMetricsCategoriesList();
        verify(ldService, times(1)).getAllMetricsCategories();
    }
}
