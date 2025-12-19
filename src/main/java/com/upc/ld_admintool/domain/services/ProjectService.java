package com.upc.ld_admintool.domain.services;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.StudentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class ProjectService {

    @Autowired
    private LDService ldService;

    @Autowired
    private LDEvalService ldEvalService;

    public List<ProjectDTO> llistarProjectesAmbStudents() {
        List<ProjectDTO> rawProjects = ldService.getAllProjects();
        List<ProjectDTO> result = new ArrayList<>();
        for (ProjectDTO p : rawProjects) {
            ProjectDTO complet = ldService.getProjectById(p.getId());
            if (complet != null) {
                result.add(complet);
            } else {
                result.add(p); 
            }
        }
        return result;
    }

    public ProjectDTO getProjectById(Long id) {
        return ldService.getProjectById(id);
    }

    public void importProjects(List<ProjectDTO> projects) {
        for (ProjectDTO project : projects) {
            Long projectId = ldService.createProject(project);
            if (projectId != null && project.getStudents() != null) {
                for (StudentDTO student : project.getStudents()) {
                    ldService.createStudent(projectId, student);
                }
            }
        }
        ldEvalService.triggerRefresh();
    }

    public void modificarProjecte(Long id, ProjectDTO projecte) {
        ProjectDTO original = ldService.getProjectById(id);

        Set<Long> originalIds = Optional.ofNullable(original.getStudents())
                .orElse(List.of()).stream()
                .map(StudentDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> newIds = Optional.ofNullable(projecte.getStudents())
                .orElse(List.of()).stream()
                .map(StudentDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 1. Identificar estudiants nous (els que tenen ID null són nous)
        List<StudentDTO> newStudents = Optional.ofNullable(projecte.getStudents())
                .orElse(List.of()).stream()
                .filter(student -> student.getId() == null)
                .collect(Collectors.toList());

        // 2. Identificar estudiants a eliminar (estan en original però no en nous)
        Set<Long> studentsToDelete = new HashSet<>(originalIds);
        studentsToDelete.removeAll(newIds);

        // 3. Eliminar estudiants
        if (!studentsToDelete.isEmpty()) {
            for (Long removedId : studentsToDelete) {
                ldService.deleteStudent(removedId);
            }
        }

        // 4. Crear estudiants nous
        if (!newStudents.isEmpty()) {
            for (StudentDTO student : newStudents) {
                System.out.println("  - Creant estudiant: " + student.getName());
                ldService.createStudent(id, student);
            }
        }
        ldService.updateProject(id, projecte);
        ldEvalService.triggerRefresh();
    }


    public void esborrarProjecte(Long id) {
        ldService.deleteProject(id);
        ldEvalService.triggerRefresh();
    }
}
