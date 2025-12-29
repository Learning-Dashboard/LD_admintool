package com.upc.ld_admintool.rest.controllers;

import java.util.List;
import java.util.Map;
import com.upc.ld_admintool.domain.services.ProjectService;
import com.upc.ld_admintool.domain.services.exceptions.SaveSyncException;
import com.upc.ld_admintool.domain.services.validation.ProjectValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.SaveSyncResponseDTO;
import com.upc.ld_admintool.rest.DTO.StudentValidationDTO;
import com.upc.ld_admintool.domain.services.validation.ValidationResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectValidationService validationService;

    // Llista tots els projectes
    @GetMapping
    public List<ProjectDTO> getAllProjects() {
        return projectService.llistarProjectesAmbStudents();
    }

    // Obtenir un projecte per id
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        ProjectDTO project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    // Valida i importa projectes (només els vàlids)
    @PostMapping
    public ResponseEntity<Map<String, Object>> importProjectsExcel(@RequestBody List<ProjectDTO> projects) {
        Map<String, Object> validationResult = validationService.validateProjectsWithDetails(projects);

        @SuppressWarnings("unchecked")
        List<ProjectDTO> validProjects = (List<ProjectDTO>) validationResult.get("validProjects");

        if (!validProjects.isEmpty()) {
            projectService.importProjects(validProjects);
        }
        return ResponseEntity.ok(validationResult);
    }

    @PostMapping("/sync-categories")
    public ResponseEntity<Void> syncCategoriesAfterImport() {
        projectService.synchronizeCategoriesAfterDataImport();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate-student")
    public ResponseEntity<ValidationResult> validateStudent(@RequestBody StudentValidationDTO request) {
        ValidationResult result = validationService.validateStudent(
                request.getGithubUrl(),
                request.getTaigaUrl(),
                request.getGithubToken(),
                request.getStudent());

        return ResponseEntity.ok(result);
    }

    // Modifica un projecte per id
    @PutMapping("/{id}")
    public ResponseEntity<?> modificarProjecte(@PathVariable Long id, @RequestBody ProjectDTO projecte) {
        if (projecte.getStudents() != null) {
            projecte.getStudents().forEach(student -> {
                System.out.println("  - " + student.getName() + " (ID: " + student.getId() + ")");
            });
        }
        try {
            SaveSyncResponseDTO response = projectService.modificarProjecte(id, projecte);
            return ResponseEntity.ok(response);
        } catch (SaveSyncException syncError) {
            return ResponseEntity.badRequest().body(syncError.getResponse());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Elimina un projecte per id
    @DeleteMapping("/{id}")
    public ResponseEntity<?> esborrarProjecte(@PathVariable Long id) {
        projectService.esborrarProjecte(id);
        return ResponseEntity.ok().build();
    }
}
