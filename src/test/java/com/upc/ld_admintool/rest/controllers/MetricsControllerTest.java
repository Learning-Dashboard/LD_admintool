package com.upc.ld_admintool.rest.controllers;

import com.upc.ld_admintool.domain.services.MetricsService;
import com.upc.ld_admintool.rest.DTO.MetricDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para MetricsController
 * Valida los endpoints REST de métricas
 */
@WebMvcTest(MetricsController.class)
@DisplayName("MetricsController - Tests de Integración")
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsService metricsService;

    private List<MetricDTO> testMetrics;
    private List<String> testCategoriesList;
    private List<Map<String, Object>> testCategoriesMap;

    @BeforeEach
    void setUp() {
        MetricDTO metric1 = new MetricDTO();
        metric1.setId("1");
        metric1.setName("Test Metric 1");
        
        MetricDTO metric2 = new MetricDTO();
        metric2.setId("2");
        metric2.setName("Test Metric 2");
        
        testMetrics = Arrays.asList(metric1, metric2);
        testCategoriesList = Arrays.asList("Performance", "Security");
        
        Map<String, Object> categoryMap = new HashMap<>();
        categoryMap.put("name", "Performance");
        categoryMap.put("metrics", 10);
        
        testCategoriesMap = Arrays.asList(categoryMap);
    }

    @Test
    @DisplayName("GET /api/metrics debe retornar métricas por proyecto")
    void testGetMetricsByProject_Success() throws Exception {
        // Arrange
        when(metricsService.getMetricsByProject("project-123")).thenReturn(testMetrics);

        // Act & Assert
        mockMvc.perform(get("/api/metrics")
                .param("prj", "project-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Test Metric 1"));

        verify(metricsService, times(1)).getMetricsByProject("project-123");
    }

    @Test
    @DisplayName("GET /api/metrics/list debe retornar lista de categorías")
    void testGetMetricsCategoriesList_Success() throws Exception {
        // Arrange
        when(metricsService.getMetricsCategoriesList()).thenReturn(testCategoriesList);

        // Act & Assert
        mockMvc.perform(get("/api/metrics/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Performance"));

        verify(metricsService, times(1)).getMetricsCategoriesList();
    }

    @Test
    @DisplayName("GET /api/metrics/categories debe retornar todas las categorías")
    void testGetAllMetricsCategories_Success() throws Exception {
        // Arrange
        when(metricsService.getAllMetricsCategories()).thenReturn(testCategoriesMap);

        // Act & Assert
        mockMvc.perform(get("/api/metrics/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Performance"))
                .andExpect(jsonPath("$[0].metrics").value(10));

        verify(metricsService, times(1)).getAllMetricsCategories();
    }

    @Test
    @DisplayName("PUT /api/metrics/{id} debe editar métrica con todos los parámetros")
    void testEditMetric_WithAllParameters() throws Exception {
        // Arrange
        doNothing().when(metricsService).editMetric(
            1L, "0.7", "http://example.com", "Performance", "global", "project-123");

        // Act & Assert
        mockMvc.perform(put("/api/metrics/1")
                .param("threshold", "0.7")
                .param("url", "http://example.com")
                .param("categoryName", "Performance")
                .param("scope", "global")
                .param("prj", "project-123"))
                .andExpect(status().isOk());

        verify(metricsService, times(1)).editMetric(
            1L, "0.7", "http://example.com", "Performance", "global", "project-123");
    }

    @Test
    @DisplayName("PUT /api/metrics/{id} debe editar métrica con parámetros opcionales null")
    void testEditMetric_WithOptionalParameters() throws Exception {
        // Arrange
        doNothing().when(metricsService).editMetric(1L, null, null, null, null, "project-123");

        // Act & Assert
        mockMvc.perform(put("/api/metrics/1")
                .param("prj", "project-123"))
                .andExpect(status().isOk());

        verify(metricsService, times(1)).editMetric(1L, null, null, null, null, "project-123");
    }

    @Test
    @DisplayName("GET /api/metrics/import debe importar métricas")
    void testImportMetrics_Success() throws Exception {
        // Arrange
        doNothing().when(metricsService).importMetrics();

        // Act & Assert
        mockMvc.perform(get("/api/metrics/import"))
                .andExpect(status().isOk());

        verify(metricsService, times(1)).importMetrics();
    }

    @Test
    @DisplayName("GET /api/metrics sin parámetro prj debe retornar 400")
    void testGetMetricsByProject_MissingParameter() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isBadRequest());

        verify(metricsService, never()).getMetricsByProject(anyString());
    }

    @Test
    @DisplayName("GET /api/metrics debe retornar lista vacía cuando no hay métricas")
    void testGetMetricsByProject_EmptyList() throws Exception {
        // Arrange
        when(metricsService.getMetricsByProject("empty-project")).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/metrics")
                .param("prj", "empty-project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(metricsService, times(1)).getMetricsByProject("empty-project");
    }

    @Test
    @DisplayName("PUT /api/metrics/{id} sin parámetro prj debe retornar 400")
    void testEditMetric_MissingRequiredParameter() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/metrics/1"))
                .andExpect(status().isBadRequest());

        verify(metricsService, never()).editMetric(anyLong(), anyString(), anyString(), 
            anyString(), anyString(), anyString());
    }
}
