package com.upc.ld_admintool.rest.controllers;

import com.upc.ld_admintool.domain.services.StrategicIndicatorsService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para StrategicIndicatorsController
 * Valida los endpoints REST de indicadores estratégicos
 */
@WebMvcTest(StrategicIndicatorsController.class)
@DisplayName("StrategicIndicatorsController - Tests de Integración")
class StrategicIndicatorsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StrategicIndicatorsService strategicIndicatorsService;

    private List<Map<String, Object>> testCategories;

    @BeforeEach
    void setUp() {
        Map<String, Object> category1 = new HashMap<>();
        category1.put("name", "Quality");
        category1.put("indicators", 5);

        Map<String, Object> category2 = new HashMap<>();
        category2.put("name", "Performance");
        category2.put("indicators", 3);

        testCategories = Arrays.asList(category1, category2);
    }

    @Test
    @DisplayName("GET /api/strategicIndicators/categories debe retornar todas las categorías")
    void testGetAllStrategicIndicatorCategories_Success() throws Exception {
        // Arrange
        when(strategicIndicatorsService.getAllStrategicIndicatorCategories()).thenReturn(testCategories);

        // Act & Assert
        mockMvc.perform(get("/api/strategicIndicators/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Quality"))
                .andExpect(jsonPath("$[0].indicators").value(5));

        verify(strategicIndicatorsService, times(1)).getAllStrategicIndicatorCategories();
    }

    @Test
    @DisplayName("GET /api/strategicIndicators/categories debe retornar lista vacía cuando no hay categorías")
    void testGetAllStrategicIndicatorCategories_EmptyList() throws Exception {
        // Arrange
        when(strategicIndicatorsService.getAllStrategicIndicatorCategories()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/strategicIndicators/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(strategicIndicatorsService, times(1)).getAllStrategicIndicatorCategories();
    }

    @Test
    @DisplayName("GET /api/strategicIndicators/fetch debe obtener indicadores estratégicos")
    void testFetchStrategicIndicators_Success() throws Exception {
        // Arrange
        doNothing().when(strategicIndicatorsService).fetchStrategicIndicators();

        // Act & Assert
        mockMvc.perform(get("/api/strategicIndicators/fetch"))
                .andExpect(status().isOk());

        verify(strategicIndicatorsService, times(1)).fetchStrategicIndicators();
    }

    @Test
    @DisplayName("GET /api/strategicIndicators/fetch debe ser idempotente")
    void testFetchStrategicIndicators_Idempotent() throws Exception {
        // Arrange
        doNothing().when(strategicIndicatorsService).fetchStrategicIndicators();

        // Act & Assert - Primera llamada
        mockMvc.perform(get("/api/strategicIndicators/fetch"))
                .andExpect(status().isOk());

        // Act & Assert - Segunda llamada
        mockMvc.perform(get("/api/strategicIndicators/fetch"))
                .andExpect(status().isOk());

        verify(strategicIndicatorsService, times(2)).fetchStrategicIndicators();
    }
}
