package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.rest.DTO.*;
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
 * Tests unitarios para WizardService
 * Valida la lógica del asistente de configuración
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WizardService - Tests Unitarios")
class WizardServiceTest {

    @Mock
    private LDService ldService;

    @InjectMocks
    private WizardService wizardService;

    private List<ProjectDTO> testProjects;
    private List<MetricDTO> testMetrics;
    private List<FactorDTO> testFactors;
    private List<Map<String, Object>> testCategories;

    @BeforeEach
    void setUp() {
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setName("Test Project");
        project.setExternalId("test-external-id");
        testProjects = Arrays.asList(project);

        MetricDTO metric = new MetricDTO();
        metric.setId("1");
        metric.setName("Test Metric");
        testMetrics = Arrays.asList(metric);

        FactorDTO factor = new FactorDTO();
        factor.setId("1");
        factor.setName("Test Factor");
        testFactors = Arrays.asList(factor);

        Map<String, Object> category = new HashMap<>();
        category.put("name", "Test Category");
        testCategories = Arrays.asList(category);
    }

    @Test
    @DisplayName("getWizardStatus debe retornar estado completo cuando todo está configurado")
    void testGetWizardStatus_AllConfigured() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(testProjects);
        when(ldService.getAllMetricsCategories()).thenReturn(testCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(testCategories);
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(testCategories);
        when(ldService.getMetricsByProject("test-external-id")).thenReturn(testMetrics);
        when(ldService.getFactorsByProject("test-external-id")).thenReturn(testFactors);

        // Act
        WizardStatusDTO status = wizardService.getWizardStatus();

        // Assert
        assertTrue(status.isHasProjects());
        assertTrue(status.isHasData());
        assertTrue(status.isHasMetricsCategories());
        assertTrue(status.isHasFactorsCategories());
        assertTrue(status.isHasStrategicIndicatorCategories());
    }

    @Test
    @DisplayName("getWizardStatus debe indicar sin proyectos cuando la lista está vacía")
    void testGetWizardStatus_NoProjects() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(Arrays.asList());
        when(ldService.getAllMetricsCategories()).thenReturn(testCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(testCategories);
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(testCategories);

        // Act
        WizardStatusDTO status = wizardService.getWizardStatus();

        // Assert
        assertFalse(status.isHasProjects());
        assertFalse(status.isHasData());
        assertTrue(status.isHasMetricsCategories());
        assertTrue(status.isHasFactorsCategories());
        assertTrue(status.isHasStrategicIndicatorCategories());
    }

    @Test
    @DisplayName("getWizardStatus debe indicar sin categorías cuando están vacías")
    void testGetWizardStatus_NoCategories() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(testProjects);
        when(ldService.getAllMetricsCategories()).thenReturn(Arrays.asList());
        when(ldService.getAllFactorsCategories()).thenReturn(Arrays.asList());
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(Arrays.asList());
        when(ldService.getMetricsByProject("test-external-id")).thenReturn(testMetrics);
        when(ldService.getFactorsByProject("test-external-id")).thenReturn(testFactors);

        // Act
        WizardStatusDTO status = wizardService.getWizardStatus();

        // Assert
        assertTrue(status.isHasProjects());
        assertTrue(status.isHasData());
        assertFalse(status.isHasMetricsCategories());
        assertFalse(status.isHasFactorsCategories());
        assertFalse(status.isHasStrategicIndicatorCategories());
    }

    @Test
    @DisplayName("getWizardStatus debe manejar categorías null")
    void testGetWizardStatus_NullCategories() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(testProjects);
        when(ldService.getAllMetricsCategories()).thenReturn(null);
        when(ldService.getAllFactorsCategories()).thenReturn(null);
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(null);
        when(ldService.getMetricsByProject("test-external-id")).thenReturn(testMetrics);
        when(ldService.getFactorsByProject("test-external-id")).thenReturn(testFactors);

        // Act
        WizardStatusDTO status = wizardService.getWizardStatus();

        // Assert
        assertTrue(status.isHasProjects());
        assertTrue(status.isHasData());
        assertFalse(status.isHasMetricsCategories());
        assertFalse(status.isHasFactorsCategories());
        assertFalse(status.isHasStrategicIndicatorCategories());
    }

    @Test
    @DisplayName("getWizardStatus debe indicar sin datos cuando métricas están vacías")
    void testGetWizardStatus_NoMetrics() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(testProjects);
        when(ldService.getAllMetricsCategories()).thenReturn(testCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(testCategories);
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(testCategories);
        when(ldService.getMetricsByProject("test-external-id")).thenReturn(Arrays.asList());
        when(ldService.getFactorsByProject("test-external-id")).thenReturn(testFactors);

        // Act
        WizardStatusDTO status = wizardService.getWizardStatus();

        // Assert
        assertTrue(status.isHasProjects());
        assertFalse(status.isHasData());
    }

    @Test
    @DisplayName("getWizardStatus debe indicar sin datos cuando factores están vacíos")
    void testGetWizardStatus_NoFactors() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(testProjects);
        when(ldService.getAllMetricsCategories()).thenReturn(testCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(testCategories);
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(testCategories);
        when(ldService.getMetricsByProject("test-external-id")).thenReturn(testMetrics);
        when(ldService.getFactorsByProject("test-external-id")).thenReturn(Arrays.asList());

        // Act
        WizardStatusDTO status = wizardService.getWizardStatus();

        // Assert
        assertTrue(status.isHasProjects());
        assertFalse(status.isHasData());
    }

    @Test
    @DisplayName("getWizardStatus debe manejar proyecto sin externalId")
    void testGetWizardStatus_ProjectWithoutExternalId() {
        // Arrange
        ProjectDTO projectWithoutExternalId = new ProjectDTO();
        projectWithoutExternalId.setId(1L);
        projectWithoutExternalId.setExternalId(null);
        
        when(ldService.getAllProjects()).thenReturn(Arrays.asList(projectWithoutExternalId));
        when(ldService.getAllMetricsCategories()).thenReturn(testCategories);
        when(ldService.getAllFactorsCategories()).thenReturn(testCategories);
        when(ldService.getAllStrategicIndicatorCategories()).thenReturn(testCategories);

        // Act
        WizardStatusDTO status = wizardService.getWizardStatus();

        // Assert
        assertTrue(status.isHasProjects());
        assertFalse(status.isHasData());
        verify(ldService, never()).getMetricsByProject(anyString());
        verify(ldService, never()).getFactorsByProject(anyString());
    }

    @Test
    @DisplayName("getWizardStatus debe manejar excepciones correctamente")
    void testGetWizardStatus_HandlesException() {
        // Arrange
        when(ldService.getAllProjects()).thenThrow(new RuntimeException("Database error"));

        // Act
        WizardStatusDTO status = wizardService.getWizardStatus();

        // Assert
        assertNotNull(status);
        assertFalse(status.isHasProjects());
        assertFalse(status.isHasData());
        assertFalse(status.isHasMetricsCategories());
        assertFalse(status.isHasFactorsCategories());
        assertFalse(status.isHasStrategicIndicatorCategories());
    }

    @Test
    @DisplayName("getWizardStatus debe manejar excepción al obtener categorías")
    void testGetWizardStatus_ExceptionInCategories() {
        // Arrange
        when(ldService.getAllProjects()).thenReturn(testProjects);
        when(ldService.getAllMetricsCategories()).thenThrow(new RuntimeException("Categories error"));

        // Act
        WizardStatusDTO status = wizardService.getWizardStatus();

        // Assert
        assertNotNull(status);
        // hasProjects se establece antes de la excepción, por lo que permanece true
        assertTrue(status.isHasProjects());
        // Los demás flags se quedan en false porque no se ejecutaron después de la excepción
        assertFalse(status.isHasData());
        assertFalse(status.isHasMetricsCategories());
        assertFalse(status.isHasFactorsCategories());
        assertFalse(status.isHasStrategicIndicatorCategories());
    }
}
