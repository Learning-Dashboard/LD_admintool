package com.upc.ld_admintool.rest.controllers;
import java.util.List;
import com.upc.ld_admintool.domain.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
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

    // Importa/crea projectes
    @PostMapping
    public ResponseEntity<?> importProjectsExcel(@RequestBody List<ProjectDTO> projects) {
        projectService.importProjects(projects);
        return ResponseEntity.ok().build();
    }

    // Modifica un projecte per id
    @PutMapping("/{id}")
    public ResponseEntity<?> modificarProjecte(@PathVariable Long id, @RequestBody ProjectDTO projecte) {
        projectService.modificarProjecte(id, projecte);
        return ResponseEntity.ok().build();
    }

    // Elimina un projecte per id
    @DeleteMapping("/{id}")
    public ResponseEntity<?> esborrarProjecte(@PathVariable Long id) {
        projectService.esborrarProjecte(id);
        return ResponseEntity.ok().build();
    }
}
