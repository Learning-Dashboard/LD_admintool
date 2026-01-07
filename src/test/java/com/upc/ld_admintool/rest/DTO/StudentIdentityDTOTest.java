package com.upc.ld_admintool.rest.DTO;

import com.upc.ld_admintool.domain.utils.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para StudentIdentityDTO
 * Valida la funcionalidad del DTO de identidad de estudiante
 */
@DisplayName("StudentIdentityDTO - Tests Unitarios")
class StudentIdentityDTOTest {

    private StudentIdentityDTO studentIdentity;

    @BeforeEach
    void setUp() {
        studentIdentity = new StudentIdentityDTO();
    }

    @Test
    @DisplayName("Debe crear instancia con constructor sin argumentos")
    void testNoArgsConstructor() {
        // Arrange & Act
        StudentIdentityDTO identity = new StudentIdentityDTO();

        // Assert
        assertNull(identity.getDataSource());
        assertNull(identity.getUsername());
    }

    @Test
    @DisplayName("Debe crear instancia con constructor con todos los argumentos")
    void testAllArgsConstructor() {
        // Act
        StudentIdentityDTO identity = new StudentIdentityDTO(DataSource.GITHUB, "johndoe");

        // Assert
        assertEquals(DataSource.GITHUB, identity.getDataSource());
        assertEquals("johndoe", identity.getUsername());
    }

    @Test
    @DisplayName("Debe configurar dataSource correctamente")
    void testSetDataSource() {
        // Act
        studentIdentity.setDataSource(DataSource.GITHUB);

        // Assert
        assertEquals(DataSource.GITHUB, studentIdentity.getDataSource());
    }

    @Test
    @DisplayName("Debe configurar username correctamente")
    void testSetUsername() {
        // Act
        studentIdentity.setUsername("testuser");

        // Assert
        assertEquals("testuser", studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe manejar diferentes DataSources")
    void testDifferentDataSources() {
        // Test GITHUB
        studentIdentity.setDataSource(DataSource.GITHUB);
        studentIdentity.setUsername("github_user");
        assertEquals(DataSource.GITHUB, studentIdentity.getDataSource());
        assertEquals("github_user", studentIdentity.getUsername());

        // Test TAIGA
        studentIdentity.setDataSource(DataSource.TAIGA);
        studentIdentity.setUsername("taiga_user");
        assertEquals(DataSource.TAIGA, studentIdentity.getDataSource());
        assertEquals("taiga_user", studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe permitir valores null")
    void testNullValues() {
        // Act
        studentIdentity.setDataSource(null);
        studentIdentity.setUsername(null);

        // Assert
        assertNull(studentIdentity.getDataSource());
        assertNull(studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe implementar equals correctamente")
    void testEquals() {
        // Arrange
        StudentIdentityDTO identity1 = new StudentIdentityDTO(DataSource.GITHUB, "user1");
        StudentIdentityDTO identity2 = new StudentIdentityDTO(DataSource.GITHUB, "user1");
        StudentIdentityDTO identity3 = new StudentIdentityDTO(DataSource.TAIGA, "user2");

        // Assert
        assertEquals(identity1, identity2);
        assertNotEquals(identity1, identity3);
        assertEquals(identity1, identity1);
        assertNotEquals(identity1, null);
    }

    @Test
    @DisplayName("Debe implementar hashCode correctamente")
    void testHashCode() {
        // Arrange
        StudentIdentityDTO identity1 = new StudentIdentityDTO(DataSource.GITHUB, "user1");
        StudentIdentityDTO identity2 = new StudentIdentityDTO(DataSource.GITHUB, "user1");

        // Assert
        assertEquals(identity1.hashCode(), identity2.hashCode());
    }

    @Test
    @DisplayName("Debe implementar toString correctamente")
    void testToString() {
        // Arrange
        studentIdentity.setDataSource(DataSource.GITHUB);
        studentIdentity.setUsername("testuser");

        // Act
        String result = studentIdentity.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("StudentIdentityDTO") || result.contains("testuser") || result.contains("GITHUB"));
    }

    @Test
    @DisplayName("Debe manejar username de GitHub")
    void testGitHubUsername() {
        // Act
        studentIdentity.setDataSource(DataSource.GITHUB);
        studentIdentity.setUsername("github_developer");

        // Assert
        assertEquals(DataSource.GITHUB, studentIdentity.getDataSource());
        assertEquals("github_developer", studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe manejar username de Taiga")
    void testTaigaUsername() {
        // Act
        studentIdentity.setDataSource(DataSource.TAIGA);
        studentIdentity.setUsername("taiga_developer");

        // Assert
        assertEquals(DataSource.TAIGA, studentIdentity.getDataSource());
        assertEquals("taiga_developer", studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe actualizar campos existentes")
    void testUpdateFields() {
        // Arrange - Initial values
        studentIdentity.setDataSource(DataSource.GITHUB);
        studentIdentity.setUsername("olduser");

        // Act - Update values
        studentIdentity.setDataSource(DataSource.TAIGA);
        studentIdentity.setUsername("newuser");

        // Assert
        assertEquals(DataSource.TAIGA, studentIdentity.getDataSource());
        assertEquals("newuser", studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe mantener consistencia de hashCode")
    void testHashCodeConsistency() {
        // Arrange
        studentIdentity.setDataSource(DataSource.GITHUB);
        studentIdentity.setUsername("user");

        // Act
        int hash1 = studentIdentity.hashCode();
        int hash2 = studentIdentity.hashCode();

        // Assert
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Debe comparar correctamente con objetos de diferente tipo")
    void testEqualsDifferentType() {
        // Arrange
        studentIdentity.setDataSource(DataSource.GITHUB);
        studentIdentity.setUsername("user");

        // Assert
        assertNotEquals(studentIdentity, new Object());
        assertNotEquals(studentIdentity, "String");
    }

    @Test
    @DisplayName("Debe manejar username vacío")
    void testEmptyUsername() {
        // Act
        studentIdentity.setUsername("");

        // Assert
        assertEquals("", studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe manejar username con caracteres especiales")
    void testUsernameWithSpecialCharacters() {
        // Act
        studentIdentity.setDataSource(DataSource.GITHUB);
        studentIdentity.setUsername("user-name_123");

        // Assert
        assertEquals("user-name_123", studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe crear múltiples instancias independientes")
    void testMultipleInstances() {
        // Arrange & Act
        StudentIdentityDTO identity1 = new StudentIdentityDTO(DataSource.GITHUB, "user1");
        StudentIdentityDTO identity2 = new StudentIdentityDTO(DataSource.TAIGA, "user2");

        // Assert
        assertNotEquals(identity1.getDataSource(), identity2.getDataSource());
        assertNotEquals(identity1.getUsername(), identity2.getUsername());
    }

    @Test
    @DisplayName("Debe permitir reasignar mismo DataSource")
    void testReassignSameDataSource() {
        // Act
        studentIdentity.setDataSource(DataSource.GITHUB);
        studentIdentity.setDataSource(DataSource.GITHUB);

        // Assert
        assertEquals(DataSource.GITHUB, studentIdentity.getDataSource());
    }

    @Test
    @DisplayName("Debe manejar cambio de username manteniendo DataSource")
    void testChangeUsernameKeepDataSource() {
        // Arrange
        studentIdentity.setDataSource(DataSource.GITHUB);
        studentIdentity.setUsername("olduser");

        // Act
        studentIdentity.setUsername("newuser");

        // Assert
        assertEquals(DataSource.GITHUB, studentIdentity.getDataSource());
        assertEquals("newuser", studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe comparar igualdad con mismo username y diferente DataSource")
    void testEqualsDifferentDataSourceSameUsername() {
        // Arrange
        StudentIdentityDTO identity1 = new StudentIdentityDTO(DataSource.GITHUB, "sameuser");
        StudentIdentityDTO identity2 = new StudentIdentityDTO(DataSource.TAIGA, "sameuser");

        // Assert
        assertNotEquals(identity1, identity2);
    }

    @Test
    @DisplayName("Debe manejar username con mayúsculas y minúsculas")
    void testUsernameCaseSensitive() {
        // Act
        studentIdentity.setUsername("UserName");

        // Assert
        assertEquals("UserName", studentIdentity.getUsername());
        assertNotEquals("username", studentIdentity.getUsername());
    }

    @Test
    @DisplayName("Debe crear identidad completa con constructor")
    void testFullConstructor() {
        // Act
        StudentIdentityDTO identity = new StudentIdentityDTO(DataSource.GITHUB, "complete_user");

        // Assert
        assertAll(
            () -> assertNotNull(identity),
            () -> assertEquals(DataSource.GITHUB, identity.getDataSource()),
            () -> assertEquals("complete_user", identity.getUsername())
        );
    }
}
