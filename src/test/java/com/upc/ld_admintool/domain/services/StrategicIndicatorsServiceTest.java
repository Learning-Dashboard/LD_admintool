package com.upc.ld_admintool.domain.services;

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
 * Tests unitarios para StrategicIndicatorsService
 * Valida la gestión de indicadores estratégicos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StrategicIndicatorsService - Tests Unitarios")
class StrategicIndicatorsServiceTest {

    @Mock
    private LDService ldService;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private StrategicIndicatorsService strategicIndicatorsService;

    private List<Map<String, Object>> testCategories;

    @BeforeEach
    void setUp() {
        // Preparar categorías de indicadores estratégicos
        Map<String, Object> category1 = new HashMap<>();
        category1.put("id", 1L);
        category1.put("name", "Quality Assurance");
        category1.put("color", "#FF5733");
        category1.put("indicators", 5);
        
        Map<String, Object> category2 = new HashMap<>();
        category2.put("id", 2L);
        category2.put("name", "Product Quality");
        category2.put("color", "#33FF57");
        category2.put("indicators", 3);
        
        testCategories = Arrays.asList(category1, category2);
    }

    @Test
    @DisplayName("Debe obtener todas las categorías de indicadores estratégicos")
    void testGetAllStrategicIndicatorCategories_Success() {
        // Arrange
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(testCategories);

        // Act
        List<Map<String, Object>> result = strategicIndicatorsService.getAllStrategicIndicatorCategories();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Quality Assurance", result.get(0).get("name"));
        assertEquals("#FF5733", result.get(0).get("color"));
        assertEquals(5, result.get(0).get("indicators"));
        verify(ldService, times(1)).getAllStrategicIndicatorCategories();
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay categorías")
    void testGetAllStrategicIndicatorCategories_EmptyList() {
        // Arrange
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(Arrays.asList());

        // Act
        List<Map<String, Object>> result = strategicIndicatorsService.getAllStrategicIndicatorCategories();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ldService, times(1)).getAllStrategicIndicatorCategories();
    }

    @Test
    @DisplayName("Debe fetchear indicadores estratégicos y sincronizar categorías")
    void testFetchStrategicIndicators_Success() {
        // Arrange
        doNothing().when(ldService).fetchStrategicIndicators();
        doNothing().when(projectService).synchronizeCategoriesAfterDataImport();

        // Act
        strategicIndicatorsService.fetchStrategicIndicators();

        // Assert
        verify(ldService, times(1)).fetchStrategicIndicators();
        verify(projectService, times(1)).synchronizeCategoriesAfterDataImport();
    }

    @Test
    @DisplayName("Debe ejecutar fetchStrategicIndicators antes de sincronizar")
    void testFetchStrategicIndicators_OrderOfExecution() {
        // Arrange
        doNothing().when(ldService).fetchStrategicIndicators();
        doNothing().when(projectService).synchronizeCategoriesAfterDataImport();

        // Act
        strategicIndicatorsService.fetchStrategicIndicators();

        // Assert - Verificar orden de invocación
        var inOrder = inOrder(ldService, projectService);
        inOrder.verify(ldService).fetchStrategicIndicators();
        inOrder.verify(projectService).synchronizeCategoriesAfterDataImport();
    }

    @Test
    @DisplayName("Debe llamar a synchronizeCategoriesAfterDataImport después de fetch")
    void testFetchStrategicIndicators_CallsSynchronize() {
        // Arrange
        doNothing().when(ldService).fetchStrategicIndicators();
        doNothing().when(projectService).synchronizeCategoriesAfterDataImport();

        // Act
        strategicIndicatorsService.fetchStrategicIndicators();

        // Assert
        verify(projectService, times(1)).synchronizeCategoriesAfterDataImport();
    }

    @Test
    @DisplayName("Debe delegar correctamente al LDService")
    void testServiceDelegation() {
        // Arrange
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(testCategories);

        // Act
        strategicIndicatorsService.getAllStrategicIndicatorCategories();

        // Assert
        verify(ldService, times(1)).getAllStrategicIndicatorCategories();
        verifyNoMoreInteractions(ldService);
    }

    @Test
    @DisplayName("Debe manejar categorías con diferentes estructuras")
    void testGetAllStrategicIndicatorCategories_DifferentStructures() {
        // Arrange
        Map<String, Object> customCategory = new HashMap<>();
        customCategory.put("name", "Custom");
        customCategory.put("extraField", "value");
        
        when(ldService.getAllStrategicIndicatorCategories())
            .thenReturn(Arrays.asList(customCategory));

        // Act
        List<Map<String, Object>> result = strategicIndicatorsService.getAllStrategicIndicatorCategories();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Custom", result.get(0).get("name"));
        assertEquals("value", result.get(0).get("extraField"));
    }
}
