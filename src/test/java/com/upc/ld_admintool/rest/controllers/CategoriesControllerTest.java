package com.upc.ld_admintool.rest.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.ld_admintool.domain.services.CategoriesService;
import com.upc.ld_admintool.rest.DTO.CategoryDTO;
import com.upc.ld_admintool.rest.DTO.IntervalDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para CategoriesController
 * Valida los endpoints REST de categorías
 */
@WebMvcTest(CategoriesController.class)
@DisplayName("CategoriesController - Tests de Integración")
class CategoriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoriesService categoriesService;

    private List<CategoryDTO> testCategories;
    private List<IntervalDTO> testIntervals;

    @BeforeEach
    void setUp() {
        CategoryDTO category1 = new CategoryDTO();
        category1.setCategory("Category 1");
        
        CategoryDTO category2 = new CategoryDTO();
        category2.setCategory("Category 2");
        
        testCategories = Arrays.asList(category1, category2);
        
        IntervalDTO interval1 = new IntervalDTO();
        interval1.setName("Interval 1");
        
        testIntervals = Arrays.asList(interval1);
    }

    @Test
    @DisplayName("POST /api/categories/metrics debe importar categorías de métricas")
    void testImportMetriquesCategories_Success() throws Exception {
        // Arrange
        doNothing().when(categoriesService).importarCategoriesMetriques(any());

        // Act & Assert
        mockMvc.perform(post("/api/categories/metrics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCategories)))
                .andExpect(status().isOk());

        verify(categoriesService, times(1)).importarCategoriesMetriques(any());
    }

    @Test
    @DisplayName("POST /api/categories/factors debe importar categorías de factores")
    void testImportFactorsCategories_Success() throws Exception {
        // Arrange
        doNothing().when(categoriesService).importarCategoriesFactors(any());

        // Act & Assert
        mockMvc.perform(post("/api/categories/factors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCategories)))
                .andExpect(status().isOk());

        verify(categoriesService, times(1)).importarCategoriesFactors(any());
    }

    @Test
    @DisplayName("POST /api/categories/strategicIndicators debe importar categorías de indicadores estratégicos")
    void testImportStrategicIndicatorsCategories_Success() throws Exception {
        // Arrange
        doNothing().when(categoriesService).importarCategoriesStrategicIndicators(any());

        // Act & Assert
        mockMvc.perform(post("/api/categories/strategicIndicators")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testIntervals)))
                .andExpect(status().isOk());

        verify(categoriesService, times(1)).importarCategoriesStrategicIndicators(any());
    }

    @Test
    @DisplayName("POST /api/categories/metrics debe retornar 400 con body inválido")
    void testImportMetriquesCategories_InvalidBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/categories/metrics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());

        verify(categoriesService, never()).importarCategoriesMetriques(any());
    }
}
