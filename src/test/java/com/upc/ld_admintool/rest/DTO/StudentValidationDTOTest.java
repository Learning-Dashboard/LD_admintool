package com.upc.ld_admintool.rest.DTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para StudentValidationDTO
 * Valida la funcionalidad del DTO usado para validación de estudiantes
 */
@DisplayName("StudentValidationDTO - Tests Unitarios")
class StudentValidationDTOTest {

    private StudentValidationDTO studentValidation;

    @BeforeEach
    void setUp() {
        studentValidation = new StudentValidationDTO();
    }

    @Test
    @DisplayName("Debe inicializar todos los campos correctamente")
    void testInitialization() {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setId(1L);
        student.setName("Test Student");

        // Act
        studentValidation.setProjectId(100L);
        studentValidation.setGithubUrl("https://github.com/testorg");
        studentValidation.setTaigaUrl("https://tree.taiga.io/project/test");
        studentValidation.setGithubToken("test-token");
        studentValidation.setStudent(student);

        // Assert
        assertEquals(100L, studentValidation.getProjectId());
        assertEquals("https://github.com/testorg", studentValidation.getGithubUrl());
        assertEquals("https://tree.taiga.io/project/test", studentValidation.getTaigaUrl());
        assertEquals("test-token", studentValidation.getGithubToken());
        assertNotNull(studentValidation.getStudent());
        assertEquals("Test Student", studentValidation.getStudent().getName());
    }

    @Test
    @DisplayName("Debe permitir valores null en campos opcionales")
    void testNullValues() {
        // Act
        studentValidation.setProjectId(null);
        studentValidation.setGithubUrl(null);
        studentValidation.setTaigaUrl(null);
        studentValidation.setGithubToken(null);
        studentValidation.setStudent(null);

        // Assert
        assertNull(studentValidation.getProjectId());
        assertNull(studentValidation.getGithubUrl());
        assertNull(studentValidation.getTaigaUrl());
        assertNull(studentValidation.getGithubToken());
        assertNull(studentValidation.getStudent());
    }

    @Test
    @DisplayName("Debe implementar equals correctamente con Lombok")
    void testEquals() {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setId(1L);
        student.setName("Test");

        StudentValidationDTO validation1 = new StudentValidationDTO();
        validation1.setProjectId(1L);
        validation1.setGithubUrl("https://github.com/test");
        validation1.setStudent(student);

        StudentValidationDTO validation2 = new StudentValidationDTO();
        validation2.setProjectId(1L);
        validation2.setGithubUrl("https://github.com/test");
        validation2.setStudent(student);

        StudentValidationDTO validation3 = new StudentValidationDTO();
        validation3.setProjectId(2L);

        // Assert
        assertEquals(validation1, validation2);
        assertNotEquals(validation1, validation3);
        assertEquals(validation1, validation1);
        assertNotEquals(validation1, null);
    }

    @Test
    @DisplayName("Debe implementar hashCode correctamente con Lombok")
    void testHashCode() {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setId(1L);

        StudentValidationDTO validation1 = new StudentValidationDTO();
        validation1.setProjectId(1L);
        validation1.setStudent(student);

        StudentValidationDTO validation2 = new StudentValidationDTO();
        validation2.setProjectId(1L);
        validation2.setStudent(student);

        // Assert
        assertEquals(validation1.hashCode(), validation2.hashCode());
    }

    @Test
    @DisplayName("Debe implementar toString correctamente con Lombok")
    void testToString() {
        // Arrange
        studentValidation.setProjectId(1L);
        studentValidation.setGithubUrl("https://github.com/test");

        // Act
        String result = studentValidation.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("StudentValidationDTO") || result.contains("projectId"));
    }

    @Test
    @DisplayName("Debe manejar múltiples campos con valores")
    void testMultipleFields() {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setId(5L);
        student.setName("John Doe");

        // Act
        studentValidation.setProjectId(100L);
        studentValidation.setGithubUrl("https://github.com/myorg");
        studentValidation.setTaigaUrl("https://tree.taiga.io/project/myproject");
        studentValidation.setGithubToken("ghp_123456");
        studentValidation.setStudent(student);

        // Assert
        assertAll(
            () -> assertEquals(100L, studentValidation.getProjectId()),
            () -> assertEquals("https://github.com/myorg", studentValidation.getGithubUrl()),
            () -> assertEquals("https://tree.taiga.io/project/myproject", studentValidation.getTaigaUrl()),
            () -> assertEquals("ghp_123456", studentValidation.getGithubToken()),
            () -> assertEquals(5L, studentValidation.getStudent().getId()),
            () -> assertEquals("John Doe", studentValidation.getStudent().getName())
        );
    }

    @Test
    @DisplayName("Debe actualizar campos individualmente")
    void testFieldUpdate() {
        // Arrange
        studentValidation.setProjectId(1L);
        studentValidation.setGithubUrl("https://github.com/old");

        // Act
        studentValidation.setProjectId(2L);
        studentValidation.setGithubUrl("https://github.com/new");

        // Assert
        assertEquals(2L, studentValidation.getProjectId());
        assertEquals("https://github.com/new", studentValidation.getGithubUrl());
    }

    @Test
    @DisplayName("Debe manejar estudiante con identidades")
    void testStudentWithIdentities() {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setId(1L);
        student.setName("Test Student");

        // Act
        studentValidation.setStudent(student);

        // Assert
        assertNotNull(studentValidation.getStudent());
        assertEquals(1L, studentValidation.getStudent().getId());
    }

    @Test
    @DisplayName("Debe permitir cambiar estudiante")
    void testChangeStudent() {
        // Arrange
        StudentDTO student1 = new StudentDTO();
        student1.setId(1L);
        student1.setName("Student 1");

        StudentDTO student2 = new StudentDTO();
        student2.setId(2L);
        student2.setName("Student 2");

        // Act
        studentValidation.setStudent(student1);
        assertEquals("Student 1", studentValidation.getStudent().getName());

        studentValidation.setStudent(student2);

        // Assert
        assertEquals("Student 2", studentValidation.getStudent().getName());
        assertEquals(2L, studentValidation.getStudent().getId());
    }

    @Test
    @DisplayName("Debe manejar tokens vacíos")
    void testEmptyToken() {
        // Act
        studentValidation.setGithubToken("");

        // Assert
        assertEquals("", studentValidation.getGithubToken());
    }

    @Test
    @DisplayName("Debe manejar URLs vacías")
    void testEmptyUrls() {
        // Act
        studentValidation.setGithubUrl("");
        studentValidation.setTaigaUrl("");

        // Assert
        assertEquals("", studentValidation.getGithubUrl());
        assertEquals("", studentValidation.getTaigaUrl());
    }

    @Test
    @DisplayName("Debe crear instancia sin valores iniciales")
    void testDefaultConstructor() {
        // Arrange & Act
        StudentValidationDTO newValidation = new StudentValidationDTO();

        // Assert
        assertNull(newValidation.getProjectId());
        assertNull(newValidation.getGithubUrl());
        assertNull(newValidation.getTaigaUrl());
        assertNull(newValidation.getGithubToken());
        assertNull(newValidation.getStudent());
    }

    @Test
    @DisplayName("Debe comparar correctamente con objetos de diferente tipo")
    void testEqualsDifferentType() {
        // Arrange
        studentValidation.setProjectId(1L);

        // Assert
        assertNotEquals(studentValidation, new Object());
        assertNotEquals(studentValidation, "String");
    }

    @Test
    @DisplayName("Debe mantener consistencia de hashCode")
    void testHashCodeConsistency() {
        // Arrange
        studentValidation.setProjectId(1L);
        studentValidation.setGithubUrl("https://github.com/test");

        // Act
        int hashCode1 = studentValidation.hashCode();
        int hashCode2 = studentValidation.hashCode();

        // Assert
        assertEquals(hashCode1, hashCode2);
    }
}
