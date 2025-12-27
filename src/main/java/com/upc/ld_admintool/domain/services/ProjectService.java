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

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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

        // 1. Identificar estudiants nous
        List<StudentDTO> newStudents = Optional.ofNullable(projecte.getStudents())
                .orElse(List.of()).stream()
                .filter(student -> student.getId() == null)
                .collect(Collectors.toList());

        // 2. Identificar estudiants a eliminar
        Set<Long> studentsToDelete = new HashSet<>(originalIds);
        studentsToDelete.removeAll(newIds);

        // Pre-validation: Calculate future size and check categories
        int currentSize = original.getStudents() != null ? original.getStudents().size() : 0;
        int futureSize = currentSize - studentsToDelete.size() + newStudents.size();

        if (!newStudents.isEmpty() || !studentsToDelete.isEmpty()) {
            validateCategoriesForNewTeamSize(id, futureSize);
        }

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

        // 5. Si s'han afegit o eliminat estudiants, importar dades i actualitzar
        // categories
        if (!newStudents.isEmpty() || !studentsToDelete.isEmpty()) {
            System.out.println("🔄 Estudiants modificats. Important dades i actualitzant categories...");

            // 1. Refresh LDEval map (sync) so it knows about new students
            ldEvalService.triggerRefresh();

            // 2. Import Data sequence
            try {
                System.out.println("  - Importing Metrics...");
                ldService.importMetrics();

                // Wait to ensure LD processes the metrics before factors need them
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("  - Importing Quality Factors...");
                ldService.importQualityFactors();

                System.out.println("  - Fetching Strategic Indicators...");
                ldService.fetchStrategicIndicators();
            } catch (Exception e) {
                System.err.println("⚠ Error durant la importació de dades automàtica: " + e.getMessage());
            }

            updateCategoriesForProject(id);
        }

        ldEvalService.triggerRefresh();
    }

    public void esborrarProjecte(Long id) {
        ldService.deleteProject(id);
        ldEvalService.triggerRefresh();
    }

    public void validateCategoriesForNewTeamSize(Long projectId, int numStudents) {
        ProjectDTO project = ldService.getProjectById(projectId);
        if (project == null)
            return;

        String projectExternalId = project.getExternalId();
        List<Map<String, Object>> metricCategories = ldService.getAllMetricsCategories();
        List<Map<String, Object>> factorCategories = ldService.getAllFactorsCategories();

        List<MetricDTO> metrics = ldService.getMetricsByProject(projectExternalId);
        for (MetricDTO metric : metrics) {
            String currentCategory = metric.getCategoryName();
            if (currentCategory == null || currentCategory.isEmpty())
                continue;

            String patternGroup = null;
            for (Map<String, Object> cat : metricCategories) {
                if (currentCategory.equals(cat.get("name"))) {
                    patternGroup = (String) cat.get("patternGroup");
                    break;
                }
            }

            if (patternGroup != null && !patternGroup.isEmpty()) {
                String newCategory = findCategoryForMembers(metricCategories, patternGroup, numStudents);
                if (newCategory == null) {
                    throw new RuntimeException("Error: La categoria per a " + numStudents +
                            " membres no existeix al patró '" + patternGroup + "'");
                }
            }
        }

        List<FactorDTO> factors = ldService.getFactorsByProject(projectExternalId);
        for (FactorDTO factor : factors) {
            String currentCategory = factor.getCategory();
            if (currentCategory == null || currentCategory.isEmpty())
                continue;

            String patternGroup = null;
            for (Map<String, Object> cat : factorCategories) {
                if (currentCategory.equals(cat.get("name"))) {
                    patternGroup = (String) cat.get("patternGroup");
                    break;
                }
            }

            if (patternGroup != null && !patternGroup.isEmpty()) {
                String newCategory = findCategoryForMembers(factorCategories, patternGroup, numStudents);
                if (newCategory == null) {
                    throw new RuntimeException("Error: La categoria per a " + numStudents +
                            " membres no existeix al patró '" + patternGroup);
                }
            }
        }
    }

    public void updateCategoriesForProject(Long projectId) {
        // Removed try-catch to allow exception propagation
        ProjectDTO project = ldService.getProjectById(projectId);
        if (project == null) {
            throw new RuntimeException("No s'ha trobat el projecte amb ID: " + projectId);
        }

        String projectExternalId = project.getExternalId();
        int numStudents = project.getStudents() != null ? project.getStudents().size() : 0;

        List<Map<String, Object>> metricCategories = ldService.getAllMetricsCategories();
        List<Map<String, Object>> factorCategories = ldService.getAllFactorsCategories();

        // Actualitzar mètriques
        List<MetricDTO> metrics = ldService.getMetricsByProject(projectExternalId);

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

                if (newCategory == null) {
                    throw new RuntimeException("Error: La categoria per a " + numStudents +
                            " membres no existeix al patró '" + patternGroup + "'");
                }

                if (!newCategory.equals(currentCategory)) {
                    ldService.editMetric(
                            Long.parseLong(metric.getId()),
                            null, // threshold
                            null, // url
                            newCategory, // categoryName
                            metric.getScope(), // scope
                            projectExternalId);
                }
            }
        }

        // Actualitzar factors
        List<FactorDTO> factors = ldService.getFactorsByProject(projectExternalId);

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

                if (newCategory == null) {
                    throw new RuntimeException("Error: La categoria per a " + numStudents +
                            " membres no existeix al patró '" + patternGroup + "'");
                }

                if (!newCategory.equals(currentCategory)) {
                    System.out.println("  - Actualitzant factor: " + factor.getExternalId() +
                            " de '" + currentCategory + "' a '" + newCategory + "'");
                    Long factorId = Long.parseLong(factor.getId());
                    ldService.updateFactorCategory(factorId, newCategory, projectExternalId);
                }
            }
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
