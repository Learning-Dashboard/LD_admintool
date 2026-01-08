package com.upc.ld_admintool.domain.services.exceptions;

import com.upc.ld_admintool.rest.DTO.SaveSyncResponseDTO;
import com.upc.ld_admintool.rest.DTO.SaveSyncStepDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para SaveSyncException
 * Valida el manejo de excepciones personalizadas
 */
@DisplayName("SaveSyncException - Tests Unitarios")
class SaveSyncExceptionTest {

    private SaveSyncResponseDTO testResponse;

    @BeforeEach
    void setUp() {
        testResponse = new SaveSyncResponseDTO();
        testResponse.setSuccess(false);
        
        SaveSyncStepDTO step1 = new SaveSyncStepDTO(1, "step1", "Detail 1", "SUCCESS", null);
        SaveSyncStepDTO step2 = new SaveSyncStepDTO(2, "step2", "Detail 2", "FAILED", "Step 2 failed");
        
        testResponse.setSteps(Arrays.asList(step1, step2));
    }

    @Test
    @DisplayName("Constructor con mensaje y response debe inicializar correctamente")
    void testConstructorWithMessageAndResponse() {
        String errorMessage = "Sync operation failed";
        
        SaveSyncException exception = new SaveSyncException(errorMessage, testResponse);
        
        assertEquals(errorMessage, exception.getMessage());
        assertNotNull(exception.getResponse());
        assertEquals(testResponse, exception.getResponse());
        assertFalse(exception.getResponse().isSuccess());
    }

    @Test
    @DisplayName("Constructor con mensaje, causa y response debe inicializar correctamente")
    void testConstructorWithMessageCauseAndResponse() {
        String errorMessage = "Sync operation failed with cause";
        Throwable cause = new RuntimeException("Root cause");
        
        SaveSyncException exception = new SaveSyncException(errorMessage, cause, testResponse);
        
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNotNull(exception.getResponse());
        assertEquals(testResponse, exception.getResponse());
    }

    @Test
    @DisplayName("getResponse debe retornar el response correcto")
    void testGetResponse() {
        SaveSyncException exception = new SaveSyncException("Error", testResponse);
        
        SaveSyncResponseDTO response = exception.getResponse();
        
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(2, response.getSteps().size());
    }

    @Test
    @DisplayName("Exception debe ser instanceof RuntimeException")
    void testIsRuntimeException() {
        SaveSyncException exception = new SaveSyncException("Error", testResponse);
        
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    @DisplayName("Exception debe mantener la cadena de causas")
    void testCauseChain() {
        RuntimeException rootCause = new RuntimeException("Root");
        IllegalStateException middleCause = new IllegalStateException("Middle", rootCause);
        SaveSyncException exception = new SaveSyncException("Top", middleCause, testResponse);
        
        assertEquals(middleCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    @DisplayName("Exception debe permitir response null")
    void testNullResponse() {
        SaveSyncException exception = new SaveSyncException("Error", (SaveSyncResponseDTO) null);
        
        assertNull(exception.getResponse());
        assertEquals("Error", exception.getMessage());
    }

    @Test
    @DisplayName("Exception debe ser serializable para logging")
    void testExceptionForLogging() {
        SaveSyncException exception = new SaveSyncException("Sync failed", testResponse);
        
        String stackTrace = exception.toString();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.contains("SaveSyncException"));
    }

    @Test
    @DisplayName("Response debe contener información de pasos fallidos")
    void testResponseWithFailedSteps() {
        SaveSyncException exception = new SaveSyncException("Sync failed", testResponse);
        
        SaveSyncResponseDTO response = exception.getResponse();
        long failedSteps = response.getSteps().stream()
                .filter(step -> "FAILED".equals(step.getStatus()))
                .count();
        
        assertEquals(1, failedSteps);
    }

    @Test
    @DisplayName("Exception debe manejar mensaje null")
    void testNullMessage() {
        SaveSyncException exception = new SaveSyncException(null, testResponse);
        
        assertNull(exception.getMessage());
        assertNotNull(exception.getResponse());
    }

    @Test
    @DisplayName("Exception con response vacío debe funcionar correctamente")
    void testEmptyResponse() {
        SaveSyncResponseDTO emptyResponse = new SaveSyncResponseDTO();
        SaveSyncException exception = new SaveSyncException("Error", emptyResponse);
        
        assertNotNull(exception.getResponse());
        assertEquals(emptyResponse, exception.getResponse());
    }
}
