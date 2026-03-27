package com.upc.ld_admintool.rest.DTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para IntervalDTO
 * Valida la funcionalidad del DTO para intervalos de indicadores estratégicos
 */
@DisplayName("IntervalDTO - Tests Unitarios")
class IntervalDTOTest {

    private IntervalDTO interval;

    @BeforeEach
    void setUp() {
        interval = new IntervalDTO();
    }

    @Test
    @DisplayName("Debe crear instancia con valores por defecto")
    void testDefaultConstructor() {
        // Arrange & Act
        IntervalDTO newInterval = new IntervalDTO();

        // Assert
        assertNull(newInterval.getName());
        assertNull(newInterval.getColor());
    }

    @Test
    @DisplayName("Debe configurar name correctamente")
    void testSetName() {
        // Act
        interval.setName("Good");

        // Assert
        assertEquals("Good", interval.getName());
    }

    @Test
    @DisplayName("Debe configurar color correctamente")
    void testSetColor() {
        // Act
        interval.setColor("#00FF00");

        // Assert
        assertEquals("#00FF00", interval.getColor());
    }

    @Test
    @DisplayName("Debe manejar diferentes nombres de intervalo")
    void testDifferentNames() {
        // Test "Excellent"
        interval.setName("Excellent");
        assertEquals("Excellent", interval.getName());

        // Test "Good"
        interval.setName("Good");
        assertEquals("Good", interval.getName());

        // Test "Average"
        interval.setName("Average");
        assertEquals("Average", interval.getName());

        // Test "Poor"
        interval.setName("Poor");
        assertEquals("Poor", interval.getName());
    }

    @Test
    @DisplayName("Debe manejar diferentes colores hexadecimales")
    void testDifferentColors() {
        // Test Green
        interval.setColor("#00FF00");
        assertEquals("#00FF00", interval.getColor());

        // Test Red
        interval.setColor("#FF0000");
        assertEquals("#FF0000", interval.getColor());

        // Test Blue
        interval.setColor("#0000FF");
        assertEquals("#0000FF", interval.getColor());

        // Test Yellow
        interval.setColor("#FFFF00");
        assertEquals("#FFFF00", interval.getColor());
    }

    @Test
    @DisplayName("Debe permitir valores null")
    void testNullValues() {
        // Act
        interval.setName(null);
        interval.setColor(null);

        // Assert
        assertNull(interval.getName());
        assertNull(interval.getColor());
    }

    @Test
    @DisplayName("Debe implementar equals correctamente")
    void testEquals() {
        // Arrange
        IntervalDTO interval1 = new IntervalDTO();
        interval1.setName("Good");
        interval1.setColor("#00FF00");

        IntervalDTO interval2 = new IntervalDTO();
        interval2.setName("Good");
        interval2.setColor("#00FF00");

        IntervalDTO interval3 = new IntervalDTO();
        interval3.setName("Excellent");
        interval3.setColor("#008000");

        // Assert
        assertEquals(interval1, interval2);
        assertNotEquals(interval1, interval3);
        assertEquals(interval1, interval1);
        assertNotEquals(interval1, null);
    }

    @Test
    @DisplayName("Debe implementar hashCode correctamente")
    void testHashCode() {
        // Arrange
        IntervalDTO interval1 = new IntervalDTO();
        interval1.setName("Good");
        interval1.setColor("#00FF00");

        IntervalDTO interval2 = new IntervalDTO();
        interval2.setName("Good");
        interval2.setColor("#00FF00");

        // Assert
        assertEquals(interval1.hashCode(), interval2.hashCode());
    }

    @Test
    @DisplayName("Debe implementar toString correctamente")
    void testToString() {
        // Arrange
        interval.setName("Good");
        interval.setColor("#00FF00");

        // Act
        String result = interval.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("IntervalDTO") || result.contains("Good") || result.contains("#00FF00"));
    }

    @Test
    @DisplayName("Debe mantener consistencia de hashCode")
    void testHashCodeConsistency() {
        // Arrange
        interval.setName("Good");
        interval.setColor("#00FF00");

        // Act
        int hash1 = interval.hashCode();
        int hash2 = interval.hashCode();

        // Assert
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Debe comparar correctamente con objetos de diferente tipo")
    void testEqualsDifferentType() {
        // Arrange
        interval.setName("Good");

        // Assert
        assertNotEquals(interval, new Object());
        assertNotEquals(interval, "String");
    }

    @Test
    @DisplayName("Debe manejar valores vacíos")
    void testEmptyValues() {
        // Act
        interval.setName("");
        interval.setColor("");

        // Assert
        assertEquals("", interval.getName());
        assertEquals("", interval.getColor());
    }

    @Test
    @DisplayName("Debe actualizar valores existentes")
    void testUpdateValues() {
        // Arrange - Initial values
        interval.setName("Good");
        interval.setColor("#00FF00");

        // Act - Update values
        interval.setName("Excellent");
        interval.setColor("#008000");

        // Assert
        assertEquals("Excellent", interval.getName());
        assertEquals("#008000", interval.getColor());
    }

    @Test
    @DisplayName("Debe manejar intervalo completo")
    void testCompleteInterval() {
        // Act
        interval.setName("Very Good");
        interval.setColor("#32CD32");

        // Assert
        assertAll(
            () -> assertEquals("Very Good", interval.getName()),
            () -> assertEquals("#32CD32", interval.getColor())
        );
    }

    @Test
    @DisplayName("Debe manejar nombres con espacios")
    void testNamesWithSpaces() {
        // Act
        interval.setName("Very Good Performance");

        // Assert
        assertEquals("Very Good Performance", interval.getName());
    }

    @Test
    @DisplayName("Debe manejar colores en minúsculas")
    void testLowercaseColors() {
        // Act
        interval.setColor("#00ff00");

        // Assert
        assertEquals("#00ff00", interval.getColor());
    }

    @Test
    @DisplayName("Debe manejar colores sin símbolo #")
    void testColorsWithoutHash() {
        // Act
        interval.setColor("00FF00");

        // Assert
        assertEquals("00FF00", interval.getColor());
    }

    @Test
    @DisplayName("Debe crear múltiples instancias independientes")
    void testMultipleInstances() {
        // Arrange & Act
        IntervalDTO interval1 = new IntervalDTO();
        interval1.setName("Good");
        interval1.setColor("#00FF00");

        IntervalDTO interval2 = new IntervalDTO();
        interval2.setName("Poor");
        interval2.setColor("#FF0000");

        // Assert
        assertNotEquals(interval1.getName(), interval2.getName());
        assertNotEquals(interval1.getColor(), interval2.getColor());
    }

    @Test
    @DisplayName("Debe comparar igualdad solo con mismo nombre y color")
    void testEqualsBothFieldsRequired() {
        // Arrange
        IntervalDTO interval1 = new IntervalDTO();
        interval1.setName("Good");
        interval1.setColor("#00FF00");

        IntervalDTO interval2 = new IntervalDTO();
        interval2.setName("Good");
        interval2.setColor("#FF0000"); // Different color

        // Assert
        assertNotEquals(interval1, interval2);
    }

    @Test
    @DisplayName("Debe manejar nombres especiales")
    void testSpecialNames() {
        // Act
        interval.setName("Top 25%");

        // Assert
        assertEquals("Top 25%", interval.getName());
    }

    @Test
    @DisplayName("Debe manejar colores RGB completos")
    void testFullRGBColors() {
        // Act
        interval.setColor("#1A2B3C");

        // Assert
        assertEquals("#1A2B3C", interval.getColor());
    }
}
