package com.upc.ld_admintool.domain.services.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para ValidationResult
 * Valida la lógica de resultados de validación
 */
@DisplayName("ValidationResult - Tests Unitarios")
class ValidationResultTest {

    private ValidationResult validationResult;

    @BeforeEach
    void setUp() {
        validationResult = new ValidationResult(true);
    }

    @Test
    @DisplayName("Constructor con parámetro debe inicializar correctamente")
    void testConstructorWithParameter() {
        ValidationResult result = new ValidationResult(true);
        
        assertTrue(result.isValid());
        assertNotNull(result.getErrors());
        assertNotNull(result.getWarnings());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("Constructor completo debe establecer todos los valores")
    void testFullConstructor() {
        List<String> errors = Arrays.asList("Error 1", "Error 2");
        List<String> warnings = Arrays.asList("Warning 1");
        
        ValidationResult result = new ValidationResult(false, errors, warnings);
        
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    @DisplayName("addError debe agregar error y marcar como inválido")
    void testAddError() {
        assertTrue(validationResult.isValid());
        
        validationResult.addError("Test error");
        
        assertFalse(validationResult.isValid());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals("Test error", validationResult.getErrors().get(0));
    }

    @Test
    @DisplayName("addWarning debe agregar advertencia sin afectar validez")
    void testAddWarning() {
        assertTrue(validationResult.isValid());
        
        validationResult.addWarning("Test warning");
        
        assertTrue(validationResult.isValid());
        assertEquals(1, validationResult.getWarnings().size());
        assertEquals("Test warning", validationResult.getWarnings().get(0));
    }

    @Test
    @DisplayName("addErrors debe agregar múltiples errores")
    void testAddErrors() {
        List<String> errors = Arrays.asList("Error 1", "Error 2", "Error 3");
        
        validationResult.addErrors(errors);
        
        assertFalse(validationResult.isValid());
        assertEquals(3, validationResult.getErrors().size());
    }

    @Test
    @DisplayName("addErrors con lista null no debe causar error")
    void testAddErrorsWithNull() {
        validationResult.addErrors(null);
        
        assertTrue(validationResult.isValid());
        assertEquals(0, validationResult.getErrors().size());
    }

    @Test
    @DisplayName("addErrors con lista vacía no debe afectar validez")
    void testAddErrorsWithEmptyList() {
        validationResult.addErrors(Arrays.asList());
        
        assertTrue(validationResult.isValid());
        assertEquals(0, validationResult.getErrors().size());
    }

    @Test
    @DisplayName("addWarnings debe agregar múltiples advertencias")
    void testAddWarnings() {
        List<String> warnings = Arrays.asList("Warning 1", "Warning 2");
        
        validationResult.addWarnings(warnings);
        
        assertTrue(validationResult.isValid());
        assertEquals(2, validationResult.getWarnings().size());
    }

    @Test
    @DisplayName("addWarnings con lista null no debe causar error")
    void testAddWarningsWithNull() {
        validationResult.addWarnings(null);
        
        assertTrue(validationResult.isValid());
        assertEquals(0, validationResult.getWarnings().size());
    }

    @Test
    @DisplayName("hasErrors debe retornar true cuando hay errores")
    void testHasErrors_WithErrors() {
        validationResult.addError("Error");
        
        assertTrue(validationResult.hasErrors());
    }

    @Test
    @DisplayName("hasErrors debe retornar false cuando no hay errores")
    void testHasErrors_NoErrors() {
        assertFalse(validationResult.hasErrors());
    }

    @Test
    @DisplayName("hasWarnings debe retornar true cuando hay advertencias")
    void testHasWarnings_WithWarnings() {
        validationResult.addWarning("Warning");
        
        assertTrue(validationResult.hasWarnings());
    }

    @Test
    @DisplayName("hasWarnings debe retornar false cuando no hay advertencias")
    void testHasWarnings_NoWarnings() {
        assertFalse(validationResult.hasWarnings());
    }

    @Test
    @DisplayName("Múltiples errores deben mantener el estado inválido")
    void testMultipleErrors() {
        validationResult.addError("Error 1");
        validationResult.addError("Error 2");
        validationResult.addError("Error 3");
        
        assertFalse(validationResult.isValid());
        assertEquals(3, validationResult.getErrors().size());
        assertTrue(validationResult.hasErrors());
    }

    @Test
    @DisplayName("Errores y advertencias pueden coexistir")
    void testErrorsAndWarningsTogether() {
        validationResult.addError("Error");
        validationResult.addWarning("Warning");
        
        assertFalse(validationResult.isValid());
        assertTrue(validationResult.hasErrors());
        assertTrue(validationResult.hasWarnings());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals(1, validationResult.getWarnings().size());
    }

    @Test
    @DisplayName("ValidationResult debe ser mutable con setters")
    void testSetters() {
        validationResult.setValid(false);
        validationResult.setErrors(Arrays.asList("New error"));
        validationResult.setWarnings(Arrays.asList("New warning"));
        
        assertFalse(validationResult.isValid());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals(1, validationResult.getWarnings().size());
    }

    @Test
    @DisplayName("equals debe retornar true para objetos iguales")
    void testEquals_SameContent() {
        ValidationResult result1 = new ValidationResult(true);
        result1.addError("Error 1");
        result1.addWarning("Warning 1");
        
        ValidationResult result2 = new ValidationResult(true);
        result2.addError("Error 1");
        result2.addWarning("Warning 1");
        
        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("equals debe retornar false para objetos diferentes")
    void testEquals_DifferentContent() {
        ValidationResult result1 = new ValidationResult(true);
        result1.addError("Error 1");
        
        ValidationResult result2 = new ValidationResult(false);
        result2.addError("Error 2");
        
        assertNotEquals(result1, result2);
    }

    @Test
    @DisplayName("equals debe retornar true para el mismo objeto")
    void testEquals_SameObject() {
        assertEquals(validationResult, validationResult);
    }

    @Test
    @DisplayName("equals debe retornar false para null")
    void testEquals_Null() {
        assertNotEquals(null, validationResult);
    }

    @Test
    @DisplayName("equals debe retornar false para clase diferente")
    void testEquals_DifferentClass() {
        assertNotEquals(validationResult, "Not a ValidationResult");
    }

    @Test
    @DisplayName("hashCode debe ser consistente para objetos iguales")
    void testHashCode_Consistency() {
        ValidationResult result1 = new ValidationResult(true);
        result1.addError("Error 1");
        
        ValidationResult result2 = new ValidationResult(true);
        result2.addError("Error 1");
        
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    @DisplayName("hashCode debe retornar el mismo valor en múltiples llamadas")
    void testHashCode_Stability() {
        int hash1 = validationResult.hashCode();
        int hash2 = validationResult.hashCode();
        
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("toString debe retornar representación no nula")
    void testToString_NotNull() {
        String result = validationResult.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("ValidationResult"));
    }

    @Test
    @DisplayName("toString debe incluir información del estado")
    void testToString_ContainsStateInfo() {
        validationResult.addError("Test Error");
        String result = validationResult.toString();
        
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    @DisplayName("canEqual debe retornar true para instancias de ValidationResult")
    void testCanEqual_SameClass() {
        ValidationResult other = new ValidationResult(true);
        
        assertTrue(validationResult.canEqual(other));
    }

    @Test
    @DisplayName("canEqual debe retornar false para objetos de otra clase")
    void testCanEqual_DifferentClass() {
        assertFalse(validationResult.canEqual("Not a ValidationResult"));
    }

    @Test
    @DisplayName("equals debe ser simétrico")
    void testEquals_Symmetric() {
        ValidationResult result1 = new ValidationResult(true);
        ValidationResult result2 = new ValidationResult(true);
        
        assertEquals(result1, result2);
        assertEquals(result2, result1);
    }

    @Test
    @DisplayName("equals debe ser transitivo")
    void testEquals_Transitive() {
        ValidationResult result1 = new ValidationResult(true);
        ValidationResult result2 = new ValidationResult(true);
        ValidationResult result3 = new ValidationResult(true);
        
        assertEquals(result1, result2);
        assertEquals(result2, result3);
        assertEquals(result1, result3);
    }
}
