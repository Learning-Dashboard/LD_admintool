package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.rest.DTO.CategoryDTO;
import com.upc.ld_admintool.rest.DTO.IntervalDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CategoriesService
 * Valida la lógica de importación de categorías para métricas, factores e indicadores estratégicos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoriesService - Tests Unitarios")
class CategoriesServiceTest {

    @Mock
    private LDService ldService;

    @InjectMocks
    private CategoriesService categoriesService;

    private List<CategoryDTO> testCategories;
    private List<IntervalDTO> testIntervals;

    @BeforeEach
    void setUp() {
        // Preparar datos de prueba
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
    @DisplayName("Debe importar categorías de métricas correctamente")
    void testImportarCategoriesMetriques_Success() {
        // Act
        categoriesService.importarCategoriesMetriques(testCategories);

        // Assert
        verify(ldService, times(1)).importarCategoriesMetriques(testCategories);
        verifyNoMoreInteractions(ldService);
    }

    @Test
    @DisplayName("Debe importar categorías de factores correctamente")
    void testImportarCategoriesFactors_Success() {
        // Act
        categoriesService.importarCategoriesFactors(testCategories);

        // Assert
        verify(ldService, times(1)).importarCategoriesFactors(testCategories);
        verifyNoMoreInteractions(ldService);
    }

    @Test
    @DisplayName("Debe importar categorías de indicadores estratégicos correctamente")
    void testImportarCategoriesStrategicIndicators_Success() {
        // Act
        categoriesService.importarCategoriesStrategicIndicators(testIntervals);

        // Assert
        verify(ldService, times(1)).importarCategoriesStrategicIndicators(testIntervals);
        verifyNoMoreInteractions(ldService);
    }

    @Test
    @DisplayName("Debe manejar lista vacía de categorías de métricas")
    void testImportarCategoriesMetriques_EmptyList() {
        // Arrange
        List<CategoryDTO> emptyList = Arrays.asList();

        // Act
        categoriesService.importarCategoriesMetriques(emptyList);

        // Assert
        verify(ldService, times(1)).importarCategoriesMetriques(emptyList);
    }
}
