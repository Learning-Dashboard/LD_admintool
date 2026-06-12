package com.upc.ld_admintool.rest.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upc.ld_admintool.domain.services.ProjectService;
import com.upc.ld_admintool.domain.services.validation.ProjectValidationService;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.StudentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para ProjectController
 * Valida los endpoints REST de proyectos
 */
@WebMvcTest(ProjectController.class)
@DisplayName("ProjectController - Tests de Integración")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ProjectValidationService validationService;

    private ProjectDTO testProject;
    private List<ProjectDTO> testProjects;

    @BeforeEach
    void setUp() {
        testProject = new ProjectDTO();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setDescription("Test Description");

        StudentDTO student = new StudentDTO();
        student.setId(1L);
        student.setName("Test Student");
        testProject.setStudents(Arrays.asList(student));

        testProjects = Arrays.asList(testProject);
    }

    @Test
    @DisplayName("GET /api/projects debe retornar todos los proyectos")
    void testGetAllProjects_Success() throws Exception {
        // Arrange
        when(projectService.llistarProjectesAmbStudents()).thenReturn(testProjects);

        // Act & Assert
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Project"));

        verify(projectService, times(1)).llistarProjectesAmbStudents();
    }

    @Test
    @DisplayName("GET /api/projects/{id} debe retornar proyecto por ID")
    void testGetProjectById_Success() throws Exception {
        // Arrange
        when(projectService.getProjectById(1L)).thenReturn(testProject);

        // Act & Assert
        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Project"));

        verify(projectService, times(1)).getProjectById(1L);
    }

    @Test
    @DisplayName("POST /api/projects debe importar proyectos válidos")
    void testImportProjectsExcel_Success() throws Exception {
        // Arrange
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("validProjects", testProjects);
        validationResult.put("invalidProjects", Arrays.asList());
        validationResult.put("totalValid", 1);
        validationResult.put("totalInvalid", 0);

        when(validationService.validateProjectsWithDetails(anyList())).thenReturn(validationResult);
        doNothing().when(projectService).importProjects(anyList());

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProjects)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValid").value(1))
                .andExpect(jsonPath("$.totalInvalid").value(0));

        verify(validationService, times(1)).validateProjectsWithDetails(anyList());
        verify(projectService, times(1)).importProjects(anyList());
    }

    @Test
    @DisplayName("POST /api/projects no debe importar si no hay proyectos válidos")
    void testImportProjectsExcel_NoValidProjects() throws Exception {
        // Arrange
        Map<String, Object> validationResult = new HashMap<>();
        validationResult.put("validProjects", Arrays.asList());
        validationResult.put("invalidProjects", testProjects);
        validationResult.put("totalValid", 0);
        validationResult.put("totalInvalid", 1);

        when(validationService.validateProjectsWithDetails(anyList())).thenReturn(validationResult);

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProjects)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValid").value(0))
                .andExpect(jsonPath("$.totalInvalid").value(1));

        verify(validationService, times(1)).validateProjectsWithDetails(anyList());
        verify(projectService, never()).importProjects(anyList());
    }

    @Test
    @DisplayName("POST /api/projects/sync-categories debe sincronizar categorías")
    void testSyncCategoriesAfterImport_Success() throws Exception {
        // Arrange
        doNothing().when(projectService).synchronizeCategoriesAfterDataImport();

        // Act & Assert
        mockMvc.perform(post("/api/projects/sync-categories"))
                .andExpect(status().isOk());

        verify(projectService, times(1)).synchronizeCategoriesAfterDataImport();
    }

    @Test
    @DisplayName("DELETE /api/projects/{id} debe eliminar proyecto")
    void testEsborrarProjecte_Success() throws Exception {
        // Arrange
        doNothing().when(projectService).esborrarProjecte(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isOk());

        verify(projectService, times(1)).esborrarProjecte(1L);
    }

    @Test
    @DisplayName("GET /api/projects debe retornar lista vacía cuando no hay proyectos")
    void testGetAllProjects_EmptyList() throws Exception {
        // Arrange
        when(projectService.llistarProjectesAmbStudents()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(projectService, times(1)).llistarProjectesAmbStudents();
    }

    @Test
    @DisplayName("POST /api/projects con body inválido debe retornar 400")
    void testImportProjectsExcel_InvalidBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());

        verify(validationService, never()).validateProjectsWithDetails(anyList());
        verify(projectService, never()).importProjects(anyList());
    }
}
