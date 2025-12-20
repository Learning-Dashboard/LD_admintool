package com.upc.ld_admintool.domain.services;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.StudentDTO;
import com.upc.ld_admintool.rest.DTO.MetricDTO;
import com.upc.ld_admintool.rest.DTO.FactorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.Map;
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
        
        // 5. Si s'han afegit o eliminat estudiants, actualitzar categories de mètriques i factors
        if (!newStudents.isEmpty() || !studentsToDelete.isEmpty()) {
            System.out.println("🔄 Estudiants modificats. Actualitzant categories...");
            updateCategoriesForProject(id);
        }
        
        ldEvalService.triggerRefresh();
    }


    public void esborrarProjecte(Long id) {
        ldService.deleteProject(id);
        ldEvalService.triggerRefresh();
    }

    public void updateCategoriesForProject(Long projectId) {
        try {
            ProjectDTO project = ldService.getProjectById(projectId);
            if (project == null) {
                System.err.println("❌ No s'ha trobat el projecte amb ID: " + projectId);
                return;
            }
            
            String projectExternalId = project.getExternalId();
            int numStudents = project.getStudents() != null ? project.getStudents().size() : 0;
            
            List<Map<String, Object>> metricCategories = ldService.getAllMetricsCategories();
            List<Map<String, Object>> factorCategories = ldService.getAllFactorsCategories();
            
            // Actualitzar mètriques
            List<MetricDTO> metrics = ldService.getMetricsByProject(projectExternalId);
            int metricsUpdated = 0;
            
            for (MetricDTO metric : metrics) {
                String currentCategory = metric.getCategoryName();
                if (currentCategory == null || currentCategory.isEmpty()) {
                    continue;
                }
                
                String patternGroup = null;
                for (Map<String, Object> cat : metricCategories) {
                    if (currentCategory.equals(cat.get("name"))) {
                        patternGroup = (String) cat.get("patternGroup");
                        break;
                    }
                }
                
                if (patternGroup != null && !patternGroup.isEmpty()) {
                    String newCategory = findCategoryForMembers(metricCategories, patternGroup, numStudents);
                    
                    if (newCategory != null && !newCategory.equals(currentCategory)) {
                        ldService.editMetric(
                            Long.parseLong(metric.getId()),
                            null,  // threshold
                            null,  // url
                            newCategory,  // categoryName
                            metric.getScope(),  // scope
                            projectExternalId
                        );
                        metricsUpdated++;
                    }
                }
            }
            
            // Actualitzar factors
            List<FactorDTO> factors = ldService.getFactorsByProject(projectExternalId);
            int factorsUpdated = 0;
            
            for (FactorDTO factor : factors) {
                String currentCategory = factor.getCategory();
                if (currentCategory == null || currentCategory.isEmpty()) {
                    continue;
                }
                
                String patternGroup = null;
                for (Map<String, Object> cat : factorCategories) {
                    if (currentCategory.equals(cat.get("name"))) {
                        patternGroup = (String) cat.get("patternGroup");
                        break;
                    }
                }
                
                if (patternGroup != null && !patternGroup.isEmpty()) {
                    String newCategory = findCategoryForMembers(factorCategories, patternGroup, numStudents);
                    
                    if (newCategory != null && !newCategory.equals(currentCategory)) {
                        System.out.println("  - Actualitzant factor: " + factor.getExternalId() + 
                                         " de '" + currentCategory + "' a '" + newCategory + "'");
                        Long factorId = Long.parseLong(factor.getId());
                        ldService.updateFactorCategory(factorId, newCategory, projectExternalId);
                        factorsUpdated++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error actualitzant categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String findCategoryForMembers(List<Map<String, Object>> categories, String patternGroup, int numMembers) {
        for (Map<String, Object> cat : categories) {
            String catPatternGroup = (String) cat.get("patternGroup");
            String catName = (String) cat.get("name");
            
            if (patternGroup.equals(catPatternGroup) && 
                catName != null && 
                catName.startsWith(numMembers + " members")) {
                return catName;
            }
        }
        return null;
    }
}
