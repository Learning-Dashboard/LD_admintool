package com.upc.ld_admintool.rest.controllers;

import com.upc.ld_admintool.domain.services.FactorsService;
import com.upc.ld_admintool.rest.DTO.FactorDTO;
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
 * Tests de integración para FactorsController
 * Valida los endpoints REST de factores
 */
@WebMvcTest(FactorsController.class)
@DisplayName("FactorsController - Tests de Integración")
class FactorsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FactorsService factorsService;

    private List<FactorDTO> testFactors;
    private List<String> testCategoriesList;
    private List<Map<String, Object>> testCategoriesMap;

    @BeforeEach
    void setUp() {
        FactorDTO factor1 = new FactorDTO();
        factor1.setId("1");
        factor1.setName("Test Factor 1");
        
        FactorDTO factor2 = new FactorDTO();
        factor2.setId("2");
        factor2.setName("Test Factor 2");
        
        testFactors = Arrays.asList(factor1, factor2);
        testCategoriesList = Arrays.asList("Category A", "Category B");
        
        Map<String, Object> categoryMap = new HashMap<>();
        categoryMap.put("name", "Category A");
        categoryMap.put("count", 5);
        
        testCategoriesMap = Arrays.asList(categoryMap);
    }

    @Test
    @DisplayName("GET /api/factors debe retornar factores por proyecto")
    void testGetFactorsByProject_Success() throws Exception {
        // Arrange
        when(factorsService.getFactorsByProject("project-123")).thenReturn(testFactors);

        // Act & Assert
        mockMvc.perform(get("/api/factors")
                .param("prj", "project-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Test Factor 1"));

        verify(factorsService, times(1)).getFactorsByProject("project-123");
    }

    @Test
    @DisplayName("GET /api/factors/list debe retornar lista de categorías")
    void testGetFactorsCategoriesList_Success() throws Exception {
        // Arrange
        when(factorsService.getFactorsCategoriesList()).thenReturn(testCategoriesList);

        // Act & Assert
        mockMvc.perform(get("/api/factors/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Category A"));

        verify(factorsService, times(1)).getFactorsCategoriesList();
    }

    @Test
    @DisplayName("GET /api/factors/categories debe retornar todas las categorías")
    void testGetAllFactorsCategories_Success() throws Exception {
        // Arrange
        when(factorsService.getAllFactorsCategories()).thenReturn(testCategoriesMap);

        // Act & Assert
        mockMvc.perform(get("/api/factors/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Category A"))
                .andExpect(jsonPath("$[0].count").value(5));

        verify(factorsService, times(1)).getAllFactorsCategories();
    }

    @Test
    @DisplayName("PUT /api/factors/{id}/category debe actualizar categoría")
    void testUpdateFactorCategory_Success() throws Exception {
        // Arrange
        doNothing().when(factorsService).updateFactorCategory(1L, "New Category", "project-123");

        // Act & Assert
        mockMvc.perform(put("/api/factors/1/category")
                .param("category", "New Category")
                .param("prj", "project-123"))
                .andExpect(status().isOk());

        verify(factorsService, times(1)).updateFactorCategory(1L, "New Category", "project-123");
    }

    @Test
    @DisplayName("GET /api/factors/import debe importar factores de calidad")
    void testImportQualityFactors_Success() throws Exception {
        // Arrange
        doNothing().when(factorsService).importQualityFactors();

        // Act & Assert
        mockMvc.perform(get("/api/factors/import"))
                .andExpect(status().isOk());

        verify(factorsService, times(1)).importQualityFactors();
    }

    @Test
    @DisplayName("GET /api/factors sin parámetro prj debe retornar 400")
    void testGetFactorsByProject_MissingParameter() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/factors"))
                .andExpect(status().isBadRequest());

        verify(factorsService, never()).getFactorsByProject(anyString());
    }

    @Test
    @DisplayName("GET /api/factors debe retornar lista vacía cuando no hay factores")
    void testGetFactorsByProject_EmptyList() throws Exception {
        // Arrange
        when(factorsService.getFactorsByProject("empty-project")).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/factors")
                .param("prj", "empty-project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(factorsService, times(1)).getFactorsByProject("empty-project");
    }
}
