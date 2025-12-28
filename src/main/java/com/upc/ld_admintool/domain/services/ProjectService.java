package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.StudentDTO;
import com.upc.ld_admintool.rest.DTO.MetricDTO;
import com.upc.ld_admintool.rest.DTO.FactorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
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
        boolean createdAny = false;
        for (ProjectDTO project : projects) {
            Long projectId = ldService.createProject(project);
            if (projectId == null) {
                continue;
            }
            createdAny = true;

            if (project.getStudents() != null) {
                for (StudentDTO student : project.getStudents()) {
                    ldService.createStudent(projectId, student);
                }
            }
        }
        if (createdAny) {
            ldEvalService.triggerRefresh();
        }
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

    public void synchronizeCategoriesAfterDataImport() {
        try {
            System.out.println("🔄 Sincronitzant categories després de la importació de dades del wizard...");
            List<ProjectDTO> summaries = ldService.getAllProjects();
            Map<String, List<ProjectDTO>> grouped = new HashMap<>();

            for (ProjectDTO summary : summaries) {
                ProjectDTO full = ldService.getProjectById(summary.getId());
                if (full == null) {
                    continue;
                }
                String subjectKey = resolveSubjectKey(full);
                if (subjectKey == null) {
                    continue;
                }
                grouped.computeIfAbsent(subjectKey, key -> new ArrayList<>()).add(full);
            }

            grouped.forEach((subject, projects) -> {
                if (projects.size() < 2) {
                    return;
                }
                projects.sort((a, b) -> Long.compare(sortableId(a), sortableId(b)));
                ProjectDTO reference = projects.get(0);
                if (reference.getExternalId() == null) {
                    return;
                }

                List<MetricDTO> referenceMetrics = ldService.getMetricsByProject(reference.getExternalId());
                List<FactorDTO> referenceFactors = ldService.getFactorsByProject(reference.getExternalId());

                for (int i = 1; i < projects.size(); i++) {
                    ProjectDTO target = projects.get(i);
                    if (target.getExternalId() == null) {
                        continue;
                    }
                    try {
                        List<MetricDTO> targetMetrics = ldService.getMetricsByProject(target.getExternalId());
                        applyMetricCategoriesFromReference(reference, referenceMetrics, target, targetMetrics);

                        List<FactorDTO> targetFactors = ldService.getFactorsByProject(target.getExternalId());
                        applyFactorCategoriesFromReference(referenceFactors, targetFactors, target.getExternalId());
                    } catch (Exception inner) {
                        System.err.println("⚠ Error alineant categories per al projecte '" + target.getName()
                                + "' dins la matèria '" + subject + "': " + inner.getMessage());
                    }
                }
            });

            ldEvalService.triggerRefresh();
        } catch (Exception e) {
            System.err.println("⚠ Error sincronitzant categories després d'importar dades: " + e.getMessage());
        }
    }

    private void applyMetricCategoriesFromReference(ProjectDTO referenceProject, List<MetricDTO> referenceMetrics,
            ProjectDTO newProject, List<MetricDTO> newMetrics) {
        if (newProject == null || referenceMetrics == null || newMetrics == null) {
            return;
        }

        MetricCategoryLookup lookup = buildMetricCategoryLookup(referenceProject, referenceMetrics);
        Set<String> newAliases = buildStudentAliases(newProject);

        System.out.println("ℹ Sincronitzant categories de mètriques: referencia="
                + (referenceProject != null ? referenceProject.getExternalId() : "?")
                + " (" + referenceMetrics.size() + " mètriques) -> nou="
                + (newProject != null ? newProject.getExternalId() : "?")
                + " (" + newMetrics.size() + " mètriques). Aliases=" + newAliases);

        int updated = 0;
        int matched = 0;
        int aliasMatches = 0;
        int noMatch = 0;

        for (MetricDTO metric : newMetrics) {
            if (metric == null || metric.getId() == null) {
                continue;
            }
            String normalizedId = normalizeMetricId(metric.getExternalId());
            if (normalizedId == null) {
                continue;
            }

            String desiredCategory = lookup.exactMatch.get(normalizedId);
            if (desiredCategory == null) {
                String baseKey = buildMetricBaseKey(normalizedId, metric.getScope(), newAliases);
                if (baseKey != null) {
                    desiredCategory = lookup.byBase.get(baseKey);
                    if (desiredCategory != null) {
                        aliasMatches++;
                        System.out.println("  • Alias match metric='" + metric.getExternalId()
                                + "' baseKey='" + baseKey + "' -> categoria='" + desiredCategory + "'");
                    }
                }
            } else {
                matched++;
            }

            if (desiredCategory != null
                    && (metric.getCategoryName() == null
                            || !metric.getCategoryName().equalsIgnoreCase(desiredCategory))) {
                try {
                    updated++;
                    ldService.editMetric(Long.parseLong(metric.getId()), null, null,
                            desiredCategory, metric.getScope(), newProject.getExternalId());
                    System.out.println("  ✓ Actualitzant mètrica '" + metric.getExternalId() + "' a categoria '"
                            + desiredCategory + "'");
                } catch (Exception e) {
                    System.err.println("⚠ Error actualitzant la mètrica " + metric.getExternalId() + ": "
                            + e.getMessage());
                }
            } else if (desiredCategory == null) {
                noMatch++;
                System.out.println("  • Sense match per mètrica '" + metric.getExternalId()
                        + "' (normalitzat='" + normalizedId + "'). Categoria actual='"
                        + metric.getCategoryName() + "'");
            }
        }

        System.out.println("ℹ Resum mètriques: exactes=" + matched + ", alias=" + aliasMatches
                + ", actualitzades=" + updated + ", senseMatch=" + noMatch);
    }

    private void applyFactorCategoriesFromReference(List<FactorDTO> referenceFactors,
            List<FactorDTO> newFactors, String projectExternalId) {
        if (referenceFactors == null || newFactors == null || projectExternalId == null) {
            return;
        }

        Map<String, String> factorCategories = referenceFactors.stream()
                .filter(f -> f.getExternalId() != null && f.getCategory() != null)
                .collect(Collectors.toMap(f -> normalizeMetricId(f.getExternalId()), FactorDTO::getCategory,
                        (first, second) -> first));

        for (FactorDTO factor : newFactors) {
            if (factor == null || factor.getId() == null || factor.getExternalId() == null) {
                continue;
            }

            String key = normalizeMetricId(factor.getExternalId());
            String desiredCategory = factorCategories.get(key);
            if (desiredCategory != null
                    && (factor.getCategory() == null || !factor.getCategory().equalsIgnoreCase(desiredCategory))) {
                try {
                    ldService.updateFactorCategory(Long.parseLong(factor.getId()), desiredCategory, projectExternalId);
                } catch (Exception e) {
                    System.err.println("⚠ Error actualitzant el factor " + factor.getExternalId() + ": "
                            + e.getMessage());
                }
            }
        }
    }

    private MetricCategoryLookup buildMetricCategoryLookup(ProjectDTO project, List<MetricDTO> metrics) {
        MetricCategoryLookup lookup = new MetricCategoryLookup();
        if (metrics == null) {
            return lookup;
        }

        Set<String> aliases = buildStudentAliases(project);
        for (MetricDTO metric : metrics) {
            if (metric == null || metric.getExternalId() == null || metric.getCategoryName() == null) {
                continue;
            }
            String normalizedId = normalizeMetricId(metric.getExternalId());
            if (normalizedId == null) {
                continue;
            }
            lookup.exactMatch.put(normalizedId, metric.getCategoryName());

            String baseKey = buildMetricBaseKey(normalizedId, metric.getScope(), aliases);
            if (baseKey != null && !lookup.byBase.containsKey(baseKey)) {
                lookup.byBase.put(baseKey, metric.getCategoryName());
            }
        }
        return lookup;
    }

    private Set<String> buildStudentAliases(ProjectDTO project) {
        Set<String> aliases = new HashSet<>();
        if (project == null || project.getStudents() == null) {
            return aliases;
        }

        project.getStudents().forEach(student -> {
            if (student == null) {
                return;
            }
            if (student.getName() != null) {
                addAliasVariants(aliases, slugify(student.getName()));
            }
            if (student.getIdentities() != null) {
                student.getIdentities().values().forEach(identity -> {
                    if (identity != null && identity.getUsername() != null) {
                        addAliasVariants(aliases, slugify(identity.getUsername()));
                    }
                });
            }
        });
        return aliases;
    }

    private void addAliasVariants(Set<String> aliases, String base) {
        if (base == null || base.isEmpty()) {
            return;
        }
        aliases.add(base);
        aliases.add(base.replace("_", ""));
    }

    private String slugify(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        return normalized.replaceAll("^_+|_+$", "");
    }

    private String normalizeMetricId(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String buildMetricBaseKey(String normalizedExternalId, String scope, Set<String> aliases) {
        if (normalizedExternalId == null || aliases.isEmpty()) {
            return null;
        }
        int idx = normalizedExternalId.indexOf('_');
        if (idx <= 0 || idx >= normalizedExternalId.length() - 1) {
            return null;
        }
        String suffix = normalizedExternalId.substring(idx + 1);
        if (!matchesAlias(suffix, aliases)) {
            return null;
        }
        String prefix = normalizedExternalId.substring(0, idx);
        String scopeKey = scope != null ? scope.toLowerCase() : "";
        return prefix + "|" + scopeKey;
    }

    private boolean matchesAlias(String candidate, Set<String> aliases) {
        if (candidate == null) {
            return false;
        }
        String clean = candidate.replaceAll("^_+|_+$", "");
        return aliases.contains(clean) || aliases.contains(clean.replace("_", ""));
    }

    private String resolveSubjectKey(ProjectDTO project) {
        if (project == null) {
            return null;
        }
        if (project.getSubject() != null && !project.getSubject().isBlank()) {
            return normalizeSubjectKey(project.getSubject());
        }
        String fromExternal = extractSubjectFromIdentifier(project.getExternalId());
        if (fromExternal != null) {
            return fromExternal;
        }
        return extractSubjectFromIdentifier(project.getName());
    }

    private String normalizeSubjectKey(String value) {
        if (value == null) {
            return null;
        }
        String slug = slugify(value);
        return slug == null ? null : slug.replace("_", "");
    }

    private String extractSubjectFromIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        String normalized = slugify(identifier);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        StringBuilder letters = new StringBuilder();
        for (char ch : normalized.toCharArray()) {
            if (Character.isLetter(ch)) {
                letters.append(ch);
            } else if (letters.length() > 0) {
                break;
            }
        }
        String result = letters.toString();
        return result.length() >= 3 ? result : null;
    }

    private long sortableId(ProjectDTO project) {
        return project != null && project.getId() != null ? project.getId() : Long.MAX_VALUE;
    }

    private static class MetricCategoryLookup {
        private final Map<String, String> exactMatch = new HashMap<>();
        private final Map<String, String> byBase = new HashMap<>();
    }
}
