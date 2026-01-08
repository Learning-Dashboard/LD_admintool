package com.upc.ld_admintool.rest.controllers;

import com.upc.ld_admintool.domain.services.WizardService;
import com.upc.ld_admintool.rest.DTO.WizardStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para WizardController
 * Valida los endpoints REST del asistente de configuración
 */
@WebMvcTest(WizardController.class)
@DisplayName("WizardController - Tests de Integración")
class WizardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WizardService wizardService;

    private WizardStatusDTO testStatus;

    @BeforeEach
    void setUp() {
        testStatus = new WizardStatusDTO(true, true, true, true, true);
    }

    @Test
    @DisplayName("GET /api/wizard/status debe retornar estado completo")
    void testGetWizardStatus_Success() throws Exception {
        // Arrange
        when(wizardService.getWizardStatus()).thenReturn(testStatus);

        // Act & Assert
        mockMvc.perform(get("/api/wizard/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProjects").value(true))
                .andExpect(jsonPath("$.hasData").value(true))
                .andExpect(jsonPath("$.hasMetricsCategories").value(true))
                .andExpect(jsonPath("$.hasFactorsCategories").value(true))
                .andExpect(jsonPath("$.hasStrategicIndicatorCategories").value(true));

        verify(wizardService, times(1)).getWizardStatus();
    }

    @Test
    @DisplayName("GET /api/wizard/status debe retornar estado parcial")
    void testGetWizardStatus_PartialConfiguration() throws Exception {
        // Arrange
        WizardStatusDTO partialStatus = new WizardStatusDTO(true, false, true, false, false);
        when(wizardService.getWizardStatus()).thenReturn(partialStatus);

        // Act & Assert
        mockMvc.perform(get("/api/wizard/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProjects").value(true))
                .andExpect(jsonPath("$.hasData").value(false))
                .andExpect(jsonPath("$.hasMetricsCategories").value(true))
                .andExpect(jsonPath("$.hasFactorsCategories").value(false))
                .andExpect(jsonPath("$.hasStrategicIndicatorCategories").value(false));

        verify(wizardService, times(1)).getWizardStatus();
    }

    @Test
    @DisplayName("GET /api/wizard/status debe retornar estado sin configuración")
    void testGetWizardStatus_NoConfiguration() throws Exception {
        // Arrange
        WizardStatusDTO emptyStatus = new WizardStatusDTO(false, false, false, false, false);
        when(wizardService.getWizardStatus()).thenReturn(emptyStatus);

        // Act & Assert
        mockMvc.perform(get("/api/wizard/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasProjects").value(false))
                .andExpect(jsonPath("$.hasData").value(false))
                .andExpect(jsonPath("$.hasMetricsCategories").value(false))
                .andExpect(jsonPath("$.hasFactorsCategories").value(false))
                .andExpect(jsonPath("$.hasStrategicIndicatorCategories").value(false));

        verify(wizardService, times(1)).getWizardStatus();
    }

    @Test
    @DisplayName("GET /api/wizard/status debe ser idempotente")
    void testGetWizardStatus_Idempotent() throws Exception {
        // Arrange
        when(wizardService.getWizardStatus()).thenReturn(testStatus);

        // Act & Assert - Primera llamada
        mockMvc.perform(get("/api/wizard/status"))
                .andExpect(status().isOk());

        // Act & Assert - Segunda llamada
        mockMvc.perform(get("/api/wizard/status"))
                .andExpect(status().isOk());

        verify(wizardService, times(2)).getWizardStatus();
    }
}
