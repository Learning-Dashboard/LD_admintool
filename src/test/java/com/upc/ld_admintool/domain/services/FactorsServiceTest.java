package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.rest.DTO.FactorDTO;
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
 * Tests unitarios para FactorsService
 * Valida la gestión de factores de calidad y sus categorías
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FactorsService - Tests Unitarios")
class FactorsServiceTest {

    @Mock
    private LDService ldService;

    @InjectMocks
    private FactorsService factorsService;

    private List<FactorDTO> testFactors;
    private List<String> testCategoriesList;
    private List<Map<String, Object>> testCategoriesMap;

    @BeforeEach
    void setUp() {
        // Preparar factores de prueba
        FactorDTO factor1 = new FactorDTO();
        factor1.setId("1");
        factor1.setName("Test Factor 1");
        
        FactorDTO factor2 = new FactorDTO();
        factor2.setId("2");
        factor2.setName("Test Factor 2");
        
        testFactors = Arrays.asList(factor1, factor2);

        // Preparar categorías
        testCategoriesList = Arrays.asList("Category A", "Category B", "Category C");

        // Preparar mapa de categorías
        Map<String, Object> categoryMap1 = new HashMap<>();
        categoryMap1.put("name", "Category A");
        categoryMap1.put("count", 5);
        
        Map<String, Object> categoryMap2 = new HashMap<>();
        categoryMap2.put("name", "Category B");
        categoryMap2.put("count", 3);
        
        testCategoriesMap = Arrays.asList(categoryMap1, categoryMap2);
    }

    @Test
    @DisplayName("Debe obtener factores por proyecto correctamente")
    void testGetFactorsByProject_Success() {
        // Arrange
        String projectId = "test-project-123";
        when(ldService.getFactorsByProject(projectId)).thenReturn(testFactors);

        // Act
        List<FactorDTO> result = factorsService.getFactorsByProject(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Factor 1", result.get(0).getName());
        assertEquals("Test Factor 2", result.get(1).getName());
        verify(ldService, times(1)).getFactorsByProject(projectId);
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay factores")
    void testGetFactorsByProject_EmptyList() {
        // Arrange
        String projectId = "empty-project";
        when(ldService.getFactorsByProject(projectId)).thenReturn(Arrays.asList());

        // Act
        List<FactorDTO> result = factorsService.getFactorsByProject(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ldService, times(1)).getFactorsByProject(projectId);
    }

    @Test
    @DisplayName("Debe obtener lista de categorías de factores")
    void testGetFactorsCategoriesList_Success() {
        // Arrange
        when(ldService.getFactorsCategoriesList()).thenReturn(testCategoriesList);

        // Act
        List<String> result = factorsService.getFactorsCategoriesList();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("Category A"));
        assertTrue(result.contains("Category B"));
        assertTrue(result.contains("Category C"));
        verify(ldService, times(1)).getFactorsCategoriesList();
    }

    @Test
    @DisplayName("Debe obtener todas las categorías de factores con detalles")
    void testGetAllFactorsCategories_Success() {
        // Arrange
        when(ldService.getAllFactorsCategories()).thenReturn(testCategoriesMap);

        // Act
        List<Map<String, Object>> result = factorsService.getAllFactorsCategories();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Category A", result.get(0).get("name"));
        assertEquals(5, result.get(0).get("count"));
        verify(ldService, times(1)).getAllFactorsCategories();
    }

    @Test
    @DisplayName("Debe actualizar categoría de factor correctamente")
    void testUpdateFactorCategory_Success() {
        // Arrange
        Long factorId = 1L;
        String category = "New Category";
        String project = "project-123";
        doNothing().when(ldService).updateFactorCategory(factorId, category, project);

        // Act
        factorsService.updateFactorCategory(factorId, category, project);

        // Assert
        verify(ldService, times(1)).updateFactorCategory(factorId, category, project);
        verifyNoMoreInteractions(ldService);
    }

    @Test
    @DisplayName("Debe importar factores de calidad correctamente")
    void testImportQualityFactors_Success() {
        // Arrange
        doNothing().when(ldService).importQualityFactors();

        // Act
        factorsService.importQualityFactors();

        // Assert
        verify(ldService, times(1)).importQualityFactors();
        verifyNoMoreInteractions(ldService);
    }

    @Test
    @DisplayName("Debe delegar correctamente al LDService")
    void testServiceDelegation() {
        // Arrange
        String projectId = "test-project";
        when(ldService.getFactorsByProject(projectId)).thenReturn(testFactors);
        when(ldService.getFactorsCategoriesList()).thenReturn(testCategoriesList);
        when(ldService.getAllFactorsCategories()).thenReturn(testCategoriesMap);

        // Act
        factorsService.getFactorsByProject(projectId);
        factorsService.getFactorsCategoriesList();
        factorsService.getAllFactorsCategories();

        // Assert
        verify(ldService, times(1)).getFactorsByProject(projectId);
        verify(ldService, times(1)).getFactorsCategoriesList();
        verify(ldService, times(1)).getAllFactorsCategories();
    }
}
