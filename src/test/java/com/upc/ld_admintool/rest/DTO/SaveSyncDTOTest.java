package com.upc.ld_admintool.rest.DTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios completos para SaveSyncResponseDTO y SaveSyncStepDTO
 * Valida la lógica de respuesta de sincronización
 */
@DisplayName("SaveSync DTOs - Tests Unitarios")
class SaveSyncDTOTest {

    private SaveSyncResponseDTO response;

    @BeforeEach
    void setUp() {
        response = new SaveSyncResponseDTO();
    }

    @Test
    @DisplayName("SaveSyncResponseDTO debe inicializarse con valores por defecto")
    void testSaveSyncResponseDTO_DefaultValues() {
        assertTrue(response.isSuccess());
        assertEquals(0, response.getFinalTeamSize());
        assertNotNull(response.getSteps());
        assertTrue(response.getSteps().isEmpty());
    }

    @Test
    @DisplayName("SaveSyncResponseDTO debe permitir establecer valores")
    void testSaveSyncResponseDTO_SetValues() {
        response.setSuccess(false);
        response.setFinalTeamSize(5);
        
        assertFalse(response.isSuccess());
        assertEquals(5, response.getFinalTeamSize());
    }

    @Test
    @DisplayName("addSuccessStep debe agregar paso exitoso")
    void testAddSuccessStep() {
        response.addSuccessStep(1, "Create Project", "Project created successfully");
        
        assertEquals(1, response.getSteps().size());
        assertTrue(response.isSuccess());
        
        SaveSyncStepDTO step = response.getSteps().get(0);
        assertEquals(1, step.getOrder());
        assertEquals("Create Project", step.getName());
        assertEquals("Project created successfully", step.getDetail());
        assertEquals("SUCCESS", step.getStatus());
        assertNull(step.getError());
    }

    @Test
    @DisplayName("addFailureStep debe agregar paso fallido y marcar como no exitoso")
    void testAddFailureStep() {
        response.addFailureStep(1, "Create Student", "Student creation", "Validation error");
        
        assertEquals(1, response.getSteps().size());
        assertFalse(response.isSuccess());
        
        SaveSyncStepDTO step = response.getSteps().get(0);
        assertEquals(1, step.getOrder());
        assertEquals("Create Student", step.getName());
        assertEquals("FAILED", step.getStatus());
        assertEquals("Validation error", step.getError());
    }

    @Test
    @DisplayName("addSkippedStep debe agregar paso omitido sin afectar success")
    void testAddSkippedStep() {
        response.addSkippedStep(1, "Optional Step", "Step was skipped");
        
        assertEquals(1, response.getSteps().size());
        assertTrue(response.isSuccess());
        
        SaveSyncStepDTO step = response.getSteps().get(0);
        assertEquals("SKIPPED", step.getStatus());
        assertNull(step.getError());
    }

    @Test
    @DisplayName("Múltiples pasos exitosos deben mantener success en true")
    void testMultipleSuccessSteps() {
        response.addSuccessStep(1, "Step 1", "Detail 1");
        response.addSuccessStep(2, "Step 2", "Detail 2");
        response.addSuccessStep(3, "Step 3", "Detail 3");
        
        assertEquals(3, response.getSteps().size());
        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("Un paso fallido debe marcar toda la respuesta como fallida")
    void testOneFailureMarksTotalAsFailed() {
        response.addSuccessStep(1, "Step 1", "Detail 1");
        response.addSuccessStep(2, "Step 2", "Detail 2");
        response.addFailureStep(3, "Step 3", "Detail 3", "Error occurred");
        
        assertEquals(3, response.getSteps().size());
        assertFalse(response.isSuccess());
    }

    @Test
    @DisplayName("SaveSyncStepDTO constructor por defecto debe inicializar correctamente")
    void testSaveSyncStepDTO_DefaultConstructor() {
        SaveSyncStepDTO step = new SaveSyncStepDTO();
        
        assertNotNull(step);
        assertEquals(0, step.getOrder());
        assertNull(step.getName());
        assertNull(step.getDetail());
        assertNull(step.getStatus());
        assertNull(step.getError());
    }

    @Test
    @DisplayName("SaveSyncStepDTO constructor con parámetros debe inicializar correctamente")
    void testSaveSyncStepDTO_ParameterizedConstructor() {
        SaveSyncStepDTO step = new SaveSyncStepDTO(1, "Test Step", "Test Detail", "SUCCESS", null);
        
        assertEquals(1, step.getOrder());
        assertEquals("Test Step", step.getName());
        assertEquals("Test Detail", step.getDetail());
        assertEquals("SUCCESS", step.getStatus());
        assertNull(step.getError());
    }

    @Test
    @DisplayName("SaveSyncStepDTO debe permitir establecer todos los valores")
    void testSaveSyncStepDTO_SetValues() {
        SaveSyncStepDTO step = new SaveSyncStepDTO();
        step.setOrder(2);
        step.setName("Update Project");
        step.setDetail("Updating project data");
        step.setStatus("FAILED");
        step.setError("Network timeout");
        
        assertEquals(2, step.getOrder());
        assertEquals("Update Project", step.getName());
        assertEquals("Updating project data", step.getDetail());
        assertEquals("FAILED", step.getStatus());
        assertEquals("Network timeout", step.getError());
    }

    @Test
    @DisplayName("SaveSyncStepDTO debe permitir verificar status")
    void testSaveSyncStepDTO_CheckStatus() {
        SaveSyncStepDTO successStep = new SaveSyncStepDTO(1, "Test", "Detail", "SUCCESS", null);
        SaveSyncStepDTO failedStep = new SaveSyncStepDTO(2, "Test", "Detail", "FAILED", "Error");
        
        assertEquals("SUCCESS", successStep.getStatus());
        assertEquals("FAILED", failedStep.getStatus());
    }

    @Test
    @DisplayName("SaveSyncResponseDTO debe permitir establecer lista de pasos")
    void testSetSteps() {
        SaveSyncStepDTO step1 = new SaveSyncStepDTO(1, "Step 1", "Detail 1", "SUCCESS", null);
        SaveSyncStepDTO step2 = new SaveSyncStepDTO(2, "Step 2", "Detail 2", "SUCCESS", null);
        
        response.setSteps(Arrays.asList(step1, step2));
        
        assertEquals(2, response.getSteps().size());
    }

    @Test
    @DisplayName("Pasos omitidos no deben afectar el resultado final")
    void testSkippedStepsDoNotAffectSuccess() {
        response.addSuccessStep(1, "Step 1", "Detail 1");
        response.addSkippedStep(2, "Step 2", "Skipped");
        response.addSkippedStep(3, "Step 3", "Skipped");
        response.addSuccessStep(4, "Step 4", "Detail 4");
        
        assertEquals(4, response.getSteps().size());
        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("Orden de pasos debe mantenerse correctamente")
    void testStepOrderPreserved() {
        response.addSuccessStep(3, "Step 3", "Detail");
        response.addSuccessStep(1, "Step 1", "Detail");
        response.addSuccessStep(2, "Step 2", "Detail");
        
        assertEquals(3, response.getSteps().get(0).getOrder());
        assertEquals(1, response.getSteps().get(1).getOrder());
        assertEquals(2, response.getSteps().get(2).getOrder());
    }

    @Test
    @DisplayName("SaveSyncResponseDTO debe manejar team size correctamente")
    void testFinalTeamSize() {
        response.setFinalTeamSize(10);
        assertEquals(10, response.getFinalTeamSize());
        
        response.setFinalTeamSize(0);
        assertEquals(0, response.getFinalTeamSize());
        
        response.setFinalTeamSize(-1);
        assertEquals(-1, response.getFinalTeamSize());
    }

    @Test
    @DisplayName("SaveSyncResponseDTO debe implementar equals correctamente")
    void testSaveSyncResponseDTO_Equals() {
        SaveSyncResponseDTO response1 = new SaveSyncResponseDTO();
        response1.setSuccess(true);
        response1.setFinalTeamSize(5);
        
        SaveSyncResponseDTO response2 = new SaveSyncResponseDTO();
        response2.setSuccess(true);
        response2.setFinalTeamSize(5);
        
        SaveSyncResponseDTO response3 = new SaveSyncResponseDTO();
        response3.setSuccess(false);
        response3.setFinalTeamSize(3);
        
        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertNotEquals(response1, null);
        assertEquals(response1, response1);
    }

    @Test
    @DisplayName("SaveSyncResponseDTO debe implementar hashCode correctamente")
    void testSaveSyncResponseDTO_HashCode() {
        SaveSyncResponseDTO response1 = new SaveSyncResponseDTO();
        response1.setSuccess(true);
        response1.setFinalTeamSize(5);
        
        SaveSyncResponseDTO response2 = new SaveSyncResponseDTO();
        response2.setSuccess(true);
        response2.setFinalTeamSize(5);
        
        assertEquals(response1.hashCode(), response2.hashCode());
        
        SaveSyncResponseDTO response3 = new SaveSyncResponseDTO();
        response3.setSuccess(false);
        
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    @DisplayName("SaveSyncResponseDTO debe implementar toString correctamente")
    void testSaveSyncResponseDTO_ToString() {
        SaveSyncResponseDTO response = new SaveSyncResponseDTO();
        response.setSuccess(true);
        response.setFinalTeamSize(5);
        
        String toString = response.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("SaveSyncResponseDTO"));
    }

    @Test
    @DisplayName("SaveSyncStepDTO debe implementar equals correctamente")
    void testSaveSyncStepDTO_Equals() {
        SaveSyncStepDTO step1 = new SaveSyncStepDTO(1, "Step 1", "Detail", "SUCCESS", null);
        SaveSyncStepDTO step2 = new SaveSyncStepDTO(1, "Step 1", "Detail", "SUCCESS", null);
        SaveSyncStepDTO step3 = new SaveSyncStepDTO(2, "Step 2", "Detail", "FAILED", "Error");
        
        assertEquals(step1, step2);
        assertNotEquals(step1, step3);
        assertNotEquals(step1, null);
        assertEquals(step1, step1);
    }

    @Test
    @DisplayName("SaveSyncStepDTO debe implementar hashCode correctamente")
    void testSaveSyncStepDTO_HashCode() {
        SaveSyncStepDTO step1 = new SaveSyncStepDTO(1, "Step 1", "Detail", "SUCCESS", null);
        SaveSyncStepDTO step2 = new SaveSyncStepDTO(1, "Step 1", "Detail", "SUCCESS", null);
        
        assertEquals(step1.hashCode(), step2.hashCode());
        
        SaveSyncStepDTO step3 = new SaveSyncStepDTO(2, "Step 2", "Detail", "FAILED", "Error");
        assertNotEquals(step1.hashCode(), step3.hashCode());
    }

    @Test
    @DisplayName("SaveSyncStepDTO debe implementar toString correctamente")
    void testSaveSyncStepDTO_ToString() {
        SaveSyncStepDTO step = new SaveSyncStepDTO(1, "Test Step", "Detail", "SUCCESS", null);
        
        String toString = step.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("SaveSyncStepDTO"));
    }
}
