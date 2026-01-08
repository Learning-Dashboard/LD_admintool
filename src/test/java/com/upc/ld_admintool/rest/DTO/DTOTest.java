package com.upc.ld_admintool.rest.DTO;

import com.upc.ld_admintool.domain.utils.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para DTOs
 * Valida la correcta funcionalidad de los objetos de transferencia de datos
 */
@DisplayName("DTOs - Tests Unitarios")
class DTOTest {

    @Test
    @DisplayName("ProjectDTO debe inicializarse correctamente")
    void testProjectDTO_Initialization() {
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setName("Test Project");
        project.setDescription("Test Description");
        project.setExternalId("ext-123");
        project.setGithubToken("github-token");

        assertEquals(1L, project.getId());
        assertEquals("Test Project", project.getName());
        assertEquals("Test Description", project.getDescription());
        assertEquals("ext-123", project.getExternalId());
        assertEquals("github-token", project.getGithubToken());
    }

    @Test
    @DisplayName("ProjectDTO debe manejar identidades")
    void testProjectDTO_Identities() {
        ProjectDTO project = new ProjectDTO();
        Map<DataSource, ProjectIdentityDTO> identities = new HashMap<>();
        
        ProjectIdentityDTO githubIdentity = new ProjectIdentityDTO();
        githubIdentity.setUrl("https://github.com/org");
        identities.put(DataSource.GITHUB, githubIdentity);
        
        project.setIdentities(identities);

        assertNotNull(project.getIdentities());
        assertEquals(1, project.getIdentities().size());
        assertTrue(project.getIdentities().containsKey(DataSource.GITHUB));
    }

    @Test
    @DisplayName("ProjectDTO debe manejar lista de estudiantes")
    void testProjectDTO_Students() {
        ProjectDTO project = new ProjectDTO();
        StudentDTO student1 = new StudentDTO();
        student1.setId(1L);
        student1.setName("Student 1");
        
        StudentDTO student2 = new StudentDTO();
        student2.setId(2L);
        student2.setName("Student 2");
        
        project.setStudents(Arrays.asList(student1, student2));

        assertNotNull(project.getStudents());
        assertEquals(2, project.getStudents().size());
        assertEquals("Student 1", project.getStudents().get(0).getName());
    }

    @Test
    @DisplayName("StudentDTO debe inicializarse correctamente")
    void testStudentDTO_Initialization() {
        StudentDTO student = new StudentDTO();
        student.setId(1L);
        student.setName("John Doe");

        assertEquals(1L, student.getId());
        assertEquals("John Doe", student.getName());
    }

    @Test
    @DisplayName("StudentDTO debe manejar identidades")
    void testStudentDTO_Identities() {
        StudentDTO student = new StudentDTO();
        Map<DataSource, StudentIdentityDTO> identities = new HashMap<>();
        
        StudentIdentityDTO githubIdentity = new StudentIdentityDTO();
        githubIdentity.setUsername("johndoe");
        identities.put(DataSource.GITHUB, githubIdentity);
        
        student.setIdentities(identities);

        assertNotNull(student.getIdentities());
        assertEquals(1, student.getIdentities().size());
        assertEquals("johndoe", student.getIdentities().get(DataSource.GITHUB).getUsername());
    }

    @Test
    @DisplayName("CategoryDTO debe inicializarse correctamente")
    void testCategoryDTO_Initialization() {
        CategoryDTO category = new CategoryDTO();
        category.setCategory("Testing");
        category.setPatternGroup("test-group");

        assertEquals("Testing", category.getCategory());
        assertEquals("test-group", category.getPatternGroup());
    }

    @Test
    @DisplayName("MetricDTO debe inicializarse correctamente")
    void testMetricDTO_Initialization() {
        MetricDTO metric = new MetricDTO();
        metric.setId("1");
        metric.setName("Code Coverage");
        metric.setDescription("Percentage of code covered");
        
        assertEquals("1", metric.getId());
        assertEquals("Code Coverage", metric.getName());
        assertEquals("Percentage of code covered", metric.getDescription());
    }

    @Test
    @DisplayName("FactorDTO debe inicializarse correctamente")
    void testFactorDTO_Initialization() {
        FactorDTO factor = new FactorDTO();
        factor.setId("1");
        factor.setName("Quality Factor");
        factor.setDescription("Quality description");
        
        assertEquals("1", factor.getId());
        assertEquals("Quality Factor", factor.getName());
        assertEquals("Quality description", factor.getDescription());
    }

    @Test
    @DisplayName("ProjectIdentityDTO debe manejar URL")
    void testProjectIdentityDTO() {
        ProjectIdentityDTO identity = new ProjectIdentityDTO();
        identity.setUrl("https://github.com/test/repo");
        
        assertEquals("https://github.com/test/repo", identity.getUrl());
    }

    @Test
    @DisplayName("StudentIdentityDTO debe manejar username")
    void testStudentIdentityDTO() {
        StudentIdentityDTO identity = new StudentIdentityDTO();
        identity.setUsername("testuser");
        
        assertEquals("testuser", identity.getUsername());
    }

    @Test
    @DisplayName("WizardStatusDTO debe inicializarse con todos los flags")
    void testWizardStatusDTO() {
        WizardStatusDTO status = new WizardStatusDTO(true, true, true, true, true);
        
        assertTrue(status.isHasProjects());
        assertTrue(status.isHasData());
        assertTrue(status.isHasMetricsCategories());
        assertTrue(status.isHasFactorsCategories());
        assertTrue(status.isHasStrategicIndicatorCategories());
    }

    @Test
    @DisplayName("WizardStatusDTO debe manejar estado parcial")
    void testWizardStatusDTO_PartialState() {
        WizardStatusDTO status = new WizardStatusDTO(true, false, true, false, true);
        
        assertTrue(status.isHasProjects());
        assertFalse(status.isHasData());
        assertTrue(status.isHasMetricsCategories());
        assertFalse(status.isHasFactorsCategories());
        assertTrue(status.isHasStrategicIndicatorCategories());
    }

    @Test
    @DisplayName("ProjectDTO debe permitir null en campos opcionales")
    void testProjectDTO_NullableFields() {
        ProjectDTO project = new ProjectDTO();
        project.setGithubToken(null);
        project.setDescription(null);
        project.setStudents(null);
        project.setIdentities(null);
        
        assertNull(project.getGithubToken());
        assertNull(project.getDescription());
        assertNull(project.getStudents());
        assertNull(project.getIdentities());
    }

    @Test
    @DisplayName("StudentDTO debe manejar lista vacía de identidades")
    void testStudentDTO_EmptyIdentities() {
        StudentDTO student = new StudentDTO();
        student.setIdentities(new HashMap<>());
        
        assertNotNull(student.getIdentities());
        assertTrue(student.getIdentities().isEmpty());
    }

    @Test
    @DisplayName("MetricDTO y FactorDTO deben soportar ID tipo String")
    void testMetricAndFactorDTO_StringId() {
        MetricDTO metric = new MetricDTO();
        metric.setId("metric-abc-123");
        
        FactorDTO factor = new FactorDTO();
        factor.setId("factor-xyz-789");
        
        assertEquals("metric-abc-123", metric.getId());
        assertEquals("factor-xyz-789", factor.getId());
    }

    @Test
    @DisplayName("IntervalDTO debe inicializarse correctamente")
    void testIntervalDTO() {
        IntervalDTO interval = new IntervalDTO();
        interval.setName("Test Interval");
        interval.setColor("#FF0000");
        
        assertEquals("Test Interval", interval.getName());
        assertEquals("#FF0000", interval.getColor());
    }

    @Test
    @DisplayName("SaveSyncResponseDTO y SaveSyncStepDTO deben tener equals y hashCode")
    void testSaveSyncDTOs_EqualsHashCode() {
        SaveSyncStepDTO step1 = new SaveSyncStepDTO(1, "Step", "Detail", "SUCCESS", null);
        SaveSyncStepDTO step2 = new SaveSyncStepDTO(1, "Step", "Detail", "SUCCESS", null);
        SaveSyncStepDTO step3 = new SaveSyncStepDTO(2, "Other", "Detail", "FAILED", "Error");
        
        assertEquals(step1, step2);
        assertEquals(step1.hashCode(), step2.hashCode());
        assertNotEquals(step1, step3);
        
        SaveSyncResponseDTO response1 = new SaveSyncResponseDTO();
        response1.setSuccess(true);
        
        SaveSyncResponseDTO response2 = new SaveSyncResponseDTO();
        response2.setSuccess(true);
        
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    @DisplayName("ProjectDTO debe implementar equals correctamente")
    void testProjectDTO_Equals() {
        ProjectDTO project1 = new ProjectDTO();
        project1.setId(1L);
        project1.setName("Test");
        
        ProjectDTO project2 = new ProjectDTO();
        project2.setId(1L);
        project2.setName("Test");
        
        ProjectDTO project3 = new ProjectDTO();
        project3.setId(2L);
        project3.setName("Different");
        
        assertEquals(project1, project2);
        assertNotEquals(project1, project3);
        assertNotEquals(project1, null);
        assertEquals(project1, project1);
    }

    @Test
    @DisplayName("ProjectDTO debe implementar hashCode correctamente")
    void testProjectDTO_HashCode() {
        ProjectDTO project1 = new ProjectDTO();
        project1.setId(1L);
        project1.setName("Test");
        
        ProjectDTO project2 = new ProjectDTO();
        project2.setId(1L);
        project2.setName("Test");
        
        assertEquals(project1.hashCode(), project2.hashCode());
    }

    @Test
    @DisplayName("ProjectDTO debe implementar toString correctamente")
    void testProjectDTO_ToString() {
        ProjectDTO project = new ProjectDTO();
        project.setId(1L);
        project.setName("Test");
        
        String toString = project.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ProjectDTO") || toString.contains("Test"));
    }

    @Test
    @DisplayName("StudentDTO debe implementar equals correctamente")
    void testStudentDTO_Equals() {
        StudentDTO student1 = new StudentDTO();
        student1.setId(1L);
        student1.setName("John");
        
        StudentDTO student2 = new StudentDTO();
        student2.setId(1L);
        student2.setName("John");
        
        assertEquals(student1, student2);
        assertEquals(student1, student1);
        assertNotEquals(student1, null);
    }

    @Test
    @DisplayName("StudentDTO debe implementar hashCode correctamente")
    void testStudentDTO_HashCode() {
        StudentDTO student1 = new StudentDTO();
        student1.setId(1L);
        student1.setName("John");
        
        StudentDTO student2 = new StudentDTO();
        student2.setId(1L);
        student2.setName("John");
        
        assertEquals(student1.hashCode(), student2.hashCode());
    }

    @Test
    @DisplayName("StudentDTO debe implementar toString correctamente")
    void testStudentDTO_ToString() {
        StudentDTO student = new StudentDTO();
        student.setId(1L);
        student.setName("John");
        
        assertNotNull(student.toString());
    }

    @Test
    @DisplayName("CategoryDTO debe implementar equals correctamente")
    void testCategoryDTO_Equals() {
        CategoryDTO category1 = new CategoryDTO();
        category1.setCategory("Testing");
        
        CategoryDTO category2 = new CategoryDTO();
        category2.setCategory("Testing");
        
        assertEquals(category1, category2);
        assertNotEquals(category1, null);
    }

    @Test
    @DisplayName("CategoryDTO debe implementar hashCode correctamente")
    void testCategoryDTO_HashCode() {
        CategoryDTO category1 = new CategoryDTO();
        category1.setCategory("Testing");
        
        CategoryDTO category2 = new CategoryDTO();
        category2.setCategory("Testing");
        
        assertEquals(category1.hashCode(), category2.hashCode());
    }

    @Test
    @DisplayName("CategoryDTO debe implementar toString correctamente")
    void testCategoryDTO_ToString() {
        CategoryDTO category = new CategoryDTO();
        category.setCategory("Testing");
        
        assertNotNull(category.toString());
    }

    @Test
    @DisplayName("MetricDTO debe implementar equals correctamente")
    void testMetricDTO_Equals() {
        MetricDTO metric1 = new MetricDTO();
        metric1.setId("1");
        metric1.setName("Coverage");
        
        MetricDTO metric2 = new MetricDTO();
        metric2.setId("1");
        metric2.setName("Coverage");
        
        assertEquals(metric1, metric2);
        assertNotEquals(metric1, null);
    }

    @Test
    @DisplayName("MetricDTO debe implementar hashCode correctamente")
    void testMetricDTO_HashCode() {
        MetricDTO metric1 = new MetricDTO();
        metric1.setId("1");
        metric1.setName("Coverage");
        
        MetricDTO metric2 = new MetricDTO();
        metric2.setId("1");
        metric2.setName("Coverage");
        
        assertEquals(metric1.hashCode(), metric2.hashCode());
    }

    @Test
    @DisplayName("MetricDTO debe implementar toString correctamente")
    void testMetricDTO_ToString() {
        MetricDTO metric = new MetricDTO();
        metric.setId("1");
        metric.setName("Coverage");
        
        assertNotNull(metric.toString());
    }

    @Test
    @DisplayName("FactorDTO debe implementar equals correctamente")
    void testFactorDTO_Equals() {
        FactorDTO factor1 = new FactorDTO();
        factor1.setId("1");
        factor1.setName("Quality");
        
        FactorDTO factor2 = new FactorDTO();
        factor2.setId("1");
        factor2.setName("Quality");
        
        assertEquals(factor1, factor2);
        assertNotEquals(factor1, null);
    }

    @Test
    @DisplayName("FactorDTO debe implementar hashCode correctamente")
    void testFactorDTO_HashCode() {
        FactorDTO factor1 = new FactorDTO();
        factor1.setId("1");
        factor1.setName("Quality");
        
        FactorDTO factor2 = new FactorDTO();
        factor2.setId("1");
        factor2.setName("Quality");
        
        assertEquals(factor1.hashCode(), factor2.hashCode());
    }

    @Test
    @DisplayName("FactorDTO debe implementar toString correctamente")
    void testFactorDTO_ToString() {
        FactorDTO factor = new FactorDTO();
        factor.setId("1");
        factor.setName("Quality");
        
        assertNotNull(factor.toString());
    }

    @Test
    @DisplayName("WizardStatusDTO debe implementar equals correctamente")
    void testWizardStatusDTO_Equals() {
        WizardStatusDTO status1 = new WizardStatusDTO(true, true, true, true, true);
        WizardStatusDTO status2 = new WizardStatusDTO(true, true, true, true, true);
        
        assertEquals(status1, status2);
        assertNotEquals(status1, null);
    }

    @Test
    @DisplayName("WizardStatusDTO debe implementar hashCode correctamente")
    void testWizardStatusDTO_HashCode() {
        WizardStatusDTO status1 = new WizardStatusDTO(true, true, true, true, true);
        WizardStatusDTO status2 = new WizardStatusDTO(true, true, true, true, true);
        
        assertEquals(status1.hashCode(), status2.hashCode());
    }

    @Test
    @DisplayName("WizardStatusDTO debe implementar toString correctamente")
    void testWizardStatusDTO_ToString() {
        WizardStatusDTO status = new WizardStatusDTO(true, true, true, true, true);
        
        assertNotNull(status.toString());
    }

    @Test
    @DisplayName("FactorDTO debe inicializarse con constructor completo")
    void testFactorDTO_AllArgsConstructor() {
        // Arrange
        List<String> metrics = Arrays.asList("metric1", "metric2");
        List<String> weights = Arrays.asList("0.5", "0.5");
        
        // Act
        FactorDTO factor = new FactorDTO("1", "ext-1", "Quality Factor", "Description", 
                                         "Category1", "0.7", "MEAN", metrics, weights);
        
        // Assert
        assertEquals("1", factor.getId());
        assertEquals("ext-1", factor.getExternalId());
        assertEquals("Quality Factor", factor.getName());
        assertEquals("Description", factor.getDescription());
        assertEquals("Category1", factor.getCategory());
        assertEquals("0.7", factor.getThreshold());
        assertEquals("MEAN", factor.getType());
        assertEquals(2, factor.getMetrics().size());
        assertEquals(2, factor.getMetricsWeights().size());
    }

    @Test
    @DisplayName("FactorDTO debe configurar threshold correctamente")
    void testFactorDTO_SetThreshold() {
        // Arrange
        FactorDTO factor = new FactorDTO();
        
        // Act
        factor.setThreshold("0.8");
        
        // Assert
        assertEquals("0.8", factor.getThreshold());
    }

    @Test
    @DisplayName("FactorDTO debe configurar type correctamente")
    void testFactorDTO_SetType() {
        // Arrange
        FactorDTO factor = new FactorDTO();
        
        // Act
        factor.setType("WEIGHTED_MEAN");
        
        // Assert
        assertEquals("WEIGHTED_MEAN", factor.getType());
    }

    @Test
    @DisplayName("FactorDTO debe configurar metrics correctamente")
    void testFactorDTO_SetMetrics() {
        // Arrange
        FactorDTO factor = new FactorDTO();
        List<String> metrics = Arrays.asList("coverage", "complexity", "duplication");
        
        // Act
        factor.setMetrics(metrics);
        
        // Assert
        assertNotNull(factor.getMetrics());
        assertEquals(3, factor.getMetrics().size());
        assertTrue(factor.getMetrics().contains("coverage"));
        assertTrue(factor.getMetrics().contains("complexity"));
    }

    @Test
    @DisplayName("FactorDTO debe configurar metricsWeights correctamente")
    void testFactorDTO_SetMetricsWeights() {
        // Arrange
        FactorDTO factor = new FactorDTO();
        List<String> weights = Arrays.asList("0.4", "0.3", "0.3");
        
        // Act
        factor.setMetricsWeights(weights);
        
        // Assert
        assertNotNull(factor.getMetricsWeights());
        assertEquals(3, factor.getMetricsWeights().size());
        assertEquals("0.4", factor.getMetricsWeights().get(0));
    }

    @Test
    @DisplayName("FactorDTO equals debe comparar todos los campos")
    void testFactorDTO_EqualsAllFields() {
        // Arrange
        List<String> metrics = Arrays.asList("m1", "m2");
        List<String> weights = Arrays.asList("0.5", "0.5");
        
        FactorDTO factor1 = new FactorDTO("1", "ext-1", "Name", "Desc", "Cat", "0.7", "MEAN", metrics, weights);
        FactorDTO factor2 = new FactorDTO("1", "ext-1", "Name", "Desc", "Cat", "0.7", "MEAN", metrics, weights);
        FactorDTO factor3 = new FactorDTO("2", "ext-2", "Other", "Desc", "Cat", "0.8", "SUM", metrics, weights);
        
        // Assert
        assertEquals(factor1, factor2);
        assertNotEquals(factor1, factor3);
        assertEquals(factor1.hashCode(), factor2.hashCode());
    }

    @Test
    @DisplayName("FactorDTO equals debe manejar campos null")
    void testFactorDTO_EqualsWithNulls() {
        // Arrange
        FactorDTO factor1 = new FactorDTO();
        FactorDTO factor2 = new FactorDTO();
        
        // Assert
        assertEquals(factor1, factor2);
        assertEquals(factor1.hashCode(), factor2.hashCode());
    }

    @Test
    @DisplayName("FactorDTO debe manejar listas vacías")
    void testFactorDTO_EmptyLists() {
        // Arrange
        FactorDTO factor = new FactorDTO();
        
        // Act
        factor.setMetrics(Arrays.asList());
        factor.setMetricsWeights(Arrays.asList());
        
        // Assert
        assertNotNull(factor.getMetrics());
        assertNotNull(factor.getMetricsWeights());
        assertTrue(factor.getMetrics().isEmpty());
        assertTrue(factor.getMetricsWeights().isEmpty());
    }

    @Test
    @DisplayName("FactorDTO toString debe contener información relevante")
    void testFactorDTO_ToStringContent() {
        // Arrange
        FactorDTO factor = new FactorDTO();
        factor.setId("factor-123");
        factor.setName("Quality Factor");
        factor.setThreshold("0.75");
        
        // Act
        String toString = factor.toString();
        
        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("FactorDTO") || toString.contains("factor-123") || toString.contains("Quality"));
    }

    @Test
    @DisplayName("FactorDTO debe soportar modificación de campos después de construcción")
    void testFactorDTO_ModifyAfterConstruction() {
        // Arrange
        FactorDTO factor = new FactorDTO("1", "ext", "Name", "Desc", "Cat", "0.5", "MEAN", null, null);
        
        // Act
        factor.setName("Updated Name");
        factor.setThreshold("0.9");
        factor.setMetrics(Arrays.asList("new-metric"));
        
        // Assert
        assertEquals("Updated Name", factor.getName());
        assertEquals("0.9", factor.getThreshold());
        assertEquals(1, factor.getMetrics().size());
    }
}

