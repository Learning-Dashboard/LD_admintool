package com.upc.ld_admintool.rest.DTO;

import com.upc.ld_admintool.domain.utils.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para ProjectIdentityDTO
 * Valida la funcionalidad del DTO de identidad de proyecto
 */
@DisplayName("ProjectIdentityDTO - Tests Unitarios")
class ProjectIdentityDTOTest {

    private ProjectIdentityDTO projectIdentity;

    @BeforeEach
    void setUp() {
        projectIdentity = new ProjectIdentityDTO();
    }

    @Test
    @DisplayName("Debe crear instancia con constructor sin argumentos")
    void testNoArgsConstructor() {
        // Arrange & Act
        ProjectIdentityDTO identity = new ProjectIdentityDTO();

        // Assert
        assertNull(identity.getDataSource());
        assertNull(identity.getUrl());
        assertNull(identity.getProject());
    }

    @Test
    @DisplayName("Debe crear instancia con constructor con todos los argumentos")
    void testAllArgsConstructor() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setName("Test Project");

        // Act
        ProjectIdentityDTO identity = new ProjectIdentityDTO(
            DataSource.GITHUB,
            "https://github.com/testorg",
            project
        );

        // Assert
        assertEquals(DataSource.GITHUB, identity.getDataSource());
        assertEquals("https://github.com/testorg", identity.getUrl());
        assertNotNull(identity.getProject());
        assertEquals("Test Project", identity.getProject().getName());
    }

    @Test
    @DisplayName("Debe configurar dataSource correctamente")
    void testSetDataSource() {
        // Act
        projectIdentity.setDataSource(DataSource.GITHUB);

        // Assert
        assertEquals(DataSource.GITHUB, projectIdentity.getDataSource());
    }

    @Test
    @DisplayName("Debe configurar URL correctamente")
    void testSetUrl() {
        // Act
        projectIdentity.setUrl("https://github.com/myorg");

        // Assert
        assertEquals("https://github.com/myorg", projectIdentity.getUrl());
    }

    @Test
    @DisplayName("Debe configurar proyecto correctamente")
    void testSetProject() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setId(100L);
        project.setName("My Project");

        // Act
        projectIdentity.setProject(project);

        // Assert
        assertNotNull(projectIdentity.getProject());
        assertEquals(100L, projectIdentity.getProject().getId());
        assertEquals("My Project", projectIdentity.getProject().getName());
    }

    @Test
    @DisplayName("Debe manejar diferentes DataSources")
    void testDifferentDataSources() {
        // Test GITHUB
        projectIdentity.setDataSource(DataSource.GITHUB);
        assertEquals(DataSource.GITHUB, projectIdentity.getDataSource());

        // Test TAIGA
        projectIdentity.setDataSource(DataSource.TAIGA);
        assertEquals(DataSource.TAIGA, projectIdentity.getDataSource());
    }

    @Test
    @DisplayName("Debe permitir valores null")
    void testNullValues() {
        // Act
        projectIdentity.setDataSource(null);
        projectIdentity.setUrl(null);
        projectIdentity.setProject(null);

        // Assert
        assertNull(projectIdentity.getDataSource());
        assertNull(projectIdentity.getUrl());
        assertNull(projectIdentity.getProject());
    }

    @Test
    @DisplayName("Debe implementar equals correctamente")
    void testEquals() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);

        ProjectIdentityDTO identity1 = new ProjectIdentityDTO(
            DataSource.GITHUB,
            "https://github.com/test",
            project
        );

        ProjectIdentityDTO identity2 = new ProjectIdentityDTO(
            DataSource.GITHUB,
            "https://github.com/test",
            project
        );

        ProjectIdentityDTO identity3 = new ProjectIdentityDTO(
            DataSource.TAIGA,
            "https://taiga.io/test",
            project
        );

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
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);

        ProjectIdentityDTO identity1 = new ProjectIdentityDTO(
            DataSource.GITHUB,
            "https://github.com/test",
            project
        );

        ProjectIdentityDTO identity2 = new ProjectIdentityDTO(
            DataSource.GITHUB,
            "https://github.com/test",
            project
        );

        // Assert
        assertEquals(identity1.hashCode(), identity2.hashCode());
    }

    @Test
    @DisplayName("Debe implementar toString correctamente")
    void testToString() {
        // Arrange
        projectIdentity.setDataSource(DataSource.GITHUB);
        projectIdentity.setUrl("https://github.com/test");

        // Act
        String result = projectIdentity.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("ProjectIdentityDTO") || result.contains("GITHUB"));
    }

    @Test
    @DisplayName("Debe manejar URL de GitHub")
    void testGitHubUrl() {
        // Act
        projectIdentity.setDataSource(DataSource.GITHUB);
        projectIdentity.setUrl("https://github.com/organization/repo");

        // Assert
        assertEquals(DataSource.GITHUB, projectIdentity.getDataSource());
        assertEquals("https://github.com/organization/repo", projectIdentity.getUrl());
    }

    @Test
    @DisplayName("Debe manejar URL de Taiga")
    void testTaigaUrl() {
        // Act
        projectIdentity.setDataSource(DataSource.TAIGA);
        projectIdentity.setUrl("https://tree.taiga.io/project/user-project");

        // Assert
        assertEquals(DataSource.TAIGA, projectIdentity.getDataSource());
        assertEquals("https://tree.taiga.io/project/user-project", projectIdentity.getUrl());
    }

    @Test
    @DisplayName("Debe actualizar todos los campos")
    void testUpdateAllFields() {
        // Arrange
        ProjectDTO project1 = new ProjectDTO();
        project1.setId(1L);
        project1.setName("Project 1");

        ProjectDTO project2 = new ProjectDTO();
        project2.setId(2L);
        project2.setName("Project 2");

        // Act - Initial values
        projectIdentity.setDataSource(DataSource.GITHUB);
        projectIdentity.setUrl("https://github.com/old");
        projectIdentity.setProject(project1);

        // Act - Update values
        projectIdentity.setDataSource(DataSource.TAIGA);
        projectIdentity.setUrl("https://taiga.io/new");
        projectIdentity.setProject(project2);

        // Assert
        assertEquals(DataSource.TAIGA, projectIdentity.getDataSource());
        assertEquals("https://taiga.io/new", projectIdentity.getUrl());
        assertEquals("Project 2", projectIdentity.getProject().getName());
    }

    @Test
    @DisplayName("Debe mantener consistencia de hashCode")
    void testHashCodeConsistency() {
        // Arrange
        projectIdentity.setDataSource(DataSource.GITHUB);
        projectIdentity.setUrl("https://github.com/test");

        // Act
        int hash1 = projectIdentity.hashCode();
        int hash2 = projectIdentity.hashCode();

        // Assert
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Debe comparar correctamente con objetos de diferente tipo")
    void testEqualsDifferentType() {
        // Arrange
        projectIdentity.setDataSource(DataSource.GITHUB);

        // Assert
        assertNotEquals(projectIdentity, new Object());
        assertNotEquals(projectIdentity, "String");
    }

    @Test
    @DisplayName("Debe crear múltiples instancias independientes")
    void testMultipleInstances() {
        // Arrange & Act
        ProjectIdentityDTO identity1 = new ProjectIdentityDTO();
        identity1.setDataSource(DataSource.GITHUB);
        identity1.setUrl("https://github.com/org1");

        ProjectIdentityDTO identity2 = new ProjectIdentityDTO();
        identity2.setDataSource(DataSource.TAIGA);
        identity2.setUrl("https://taiga.io/org2");

        // Assert
        assertNotEquals(identity1.getDataSource(), identity2.getDataSource());
        assertNotEquals(identity1.getUrl(), identity2.getUrl());
    }

    @Test
    @DisplayName("Debe manejar proyecto con identidades")
    void testProjectWithIdentities() {
        // Arrange
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setName("Complex Project");
        project.setExternalId("ext-123");

        // Act
        projectIdentity.setProject(project);

        // Assert
        assertEquals("ext-123", projectIdentity.getProject().getExternalId());
    }
}
