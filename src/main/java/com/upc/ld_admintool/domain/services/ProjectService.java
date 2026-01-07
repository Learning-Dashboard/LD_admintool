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

import com.upc.ld_admintool.domain.services.exceptions.SaveSyncException;
import com.upc.ld_admintool.rest.DTO.SaveSyncResponseDTO;

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

    public SaveSyncResponseDTO modificarProjecte(Long id, ProjectDTO projecte) {
        SaveSyncResponseDTO workflow = new SaveSyncResponseDTO();
        int stepOrder = 1;

        ProjectDTO original = ldService.getProjectById(id);
        if (original == null) {
            throw new SaveSyncException("No s'ha trobat el projecte", workflow);
        }
        String projectExternalId = original != null ? original.getExternalId() : null;

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

        List<StudentDTO> newStudents = Optional.ofNullable(projecte.getStudents())
                .orElse(List.of()).stream()
                .filter(student -> student.getId() == null)
                .collect(Collectors.toList());

        Set<Long> studentsToDelete = new HashSet<>(originalIds);
        studentsToDelete.removeAll(newIds);

        int futureSize = Optional.ofNullable(projecte.getStudents()).map(List::size).orElse(0);
        workflow.setFinalTeamSize(futureSize);

        try {
            validateCategoriesForNewTeamSize(id, futureSize);
            workflow.addSuccessStep(stepOrder++, "Validate category availability",
                    "Category definitions available for " + futureSize + " member(s).");
        } catch (RuntimeException e) {
            workflow.addFailureStep(stepOrder, "Validate category availability",
                    "Unable to find categories for " + futureSize + " member(s).", e.getMessage());
            throw new SaveSyncException("Category validation failed", e, workflow);
        }

        int removed = studentsToDelete.size();
        int added = newStudents.size();

        try {
            if (!studentsToDelete.isEmpty()) {
                for (Long removedId : studentsToDelete) {
                    ldService.deleteStudent(removedId);
                }
            }

            if (!newStudents.isEmpty()) {
                for (StudentDTO student : newStudents) {
                    ldService.createStudent(id, student);
                }
            }

            ldService.updateProject(id, projecte);

            String detail = String.format("Team now has %d member(s). Added: %d, removed: %d.",
                    futureSize, added, removed);
            workflow.addSuccessStep(stepOrder++, "Update LD team roster", detail);
        } catch (RuntimeException e) {
            workflow.addFailureStep(stepOrder, "Update LD team roster",
                    "Error updating roster in LD.", e.getMessage());
            throw new SaveSyncException("Roster update failed", e, workflow);
        }

        boolean refreshOk = ldEvalService.triggerRefresh();
        if (!refreshOk) {
            workflow.addFailureStep(stepOrder, "Refresh LDEval caches",
                    "LDEval refresh endpoint returned an error.", "LDEval refresh failed");
            throw new SaveSyncException("LDEval refresh failed", workflow);
        }
        workflow.addSuccessStep(stepOrder++, "Refresh LDEval caches",
                "Learning Dashboard has been instructed to refresh its evaluation cache.");

        runDataImportSequenceWithRetry(projectExternalId, newStudents, workflow, stepOrder++);

        try {
            updateCategoriesForProject(id, projecte, futureSize);
            workflow.addSuccessStep(stepOrder++, "Reassign metric & factor categories",
                    "Categories recalculated for " + futureSize + " member(s).");
        } catch (RuntimeException e) {
            workflow.addFailureStep(stepOrder, "Reassign metric & factor categories",
                    "Unable to update categories.", e.getMessage());
            throw new SaveSyncException("Category update failed", e, workflow);
        }

        ldEvalService.triggerRefresh();
        return workflow;
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
        updateCategoriesForProject(projectId, null, null);
    }

    public void updateCategoriesForProject(Long projectId, ProjectDTO overrideProject, Integer overrideStudentCount) {
        // Removed try-catch to allow exception propagation
        ProjectDTO project = overrideProject != null ? overrideProject : ldService.getProjectById(projectId);
        if (project == null) {
            throw new RuntimeException("No s'ha trobat el projecte amb ID: " + projectId);
        }

    String projectExternalId = project.getExternalId();
    int numStudents = overrideStudentCount != null ? overrideStudentCount
        : (project.getStudents() != null ? project.getStudents().size() : 0);

        List<Map<String, Object>> metricCategories = ldService.getAllMetricsCategories();
        List<Map<String, Object>> factorCategories = ldService.getAllFactorsCategories();

        // Actualitzar mètriques
        List<MetricDTO> metrics = ldService.getMetricsByProject(projectExternalId);
        Set<String> projectAliases = overrideProject != null
                ? buildStudentAliases(overrideProject.getStudents())
                : buildStudentAliases(project);
    Map<String, String> aliasCategoryLookup = buildMetricAliasCategoryLookup(metrics, projectAliases);

        for (MetricDTO metric : metrics) {
            String currentCategory = metric.getCategoryName();
            if (needsAliasCategory(currentCategory)) {
                String desired = resolveCategoryFromAlias(metric, projectAliases, aliasCategoryLookup);
                if (desired != null && !desired.equalsIgnoreCase(currentCategory)) {
                    updateMetricCategory(metric, desired, projectExternalId);
                }
            }

            currentCategory = metric.getCategoryName();
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
                    updateMetricCategory(metric, newCategory, projectExternalId);
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

    private void runDataImportSequenceWithRetry(String projectExternalId, List<StudentDTO> newStudents,
            SaveSyncResponseDTO workflow, int stepOrder) {
        boolean needVerification = projectExternalId != null && newStudents != null && !newStudents.isEmpty();
        int maxAttempts = needVerification ? 3 : 1;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                executeDataImportCycle();
            } catch (RuntimeException e) {
                workflow.addFailureStep(stepOrder, "Re-import metrics, factors & indicators",
                        "Error while importing new data.", e.getMessage());
                throw new SaveSyncException("Data import failed", e, workflow);
            }

            if (!needVerification || metricsAvailableForStudents(projectExternalId, newStudents)) {
                String detail = attempt == 1
                        ? "Metrics, quality factors and strategic indicators re-imported successfully."
                        : "Data re-import succeeded after retry #" + attempt + ".";
                workflow.addSuccessStep(stepOrder, "Re-import metrics, factors & indicators", detail);
                return;
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    workflow.addFailureStep(stepOrder, "Re-import metrics, factors & indicators",
                            "Interrupted while waiting for metrics.", ie.getMessage());
                    throw new SaveSyncException("Interrupted while waiting for metrics", ie, workflow);
                }
            }
        }

        workflow.addFailureStep(stepOrder, "Re-import metrics, factors & indicators",
                "Metrics for the new students were not detected after multiple import attempts.",
                "New metrics not found after retries.");
        throw new SaveSyncException("New metrics not detected after import retries", workflow);
    }

    private void executeDataImportCycle() {
        ldService.importMetrics();
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting between metric and factor imports", e);
        }
        ldService.importQualityFactors();
        ldService.fetchStrategicIndicators();
    }

    private boolean metricsAvailableForStudents(String projectExternalId, List<StudentDTO> students) {
        if (projectExternalId == null || students == null || students.isEmpty()) {
            return true;
        }
        List<MetricDTO> metrics = ldService.getMetricsByProject(projectExternalId);
        if (metrics == null || metrics.isEmpty()) {
            return false;
        }
        for (StudentDTO student : students) {
            Set<String> aliases = extractAliasesForStudent(student);
            if (aliases.isEmpty()) {
                continue;
            }
            boolean found = metrics.stream().anyMatch(metric -> metricMatchesAliases(metric, aliases));
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean metricMatchesAliases(MetricDTO metric, Set<String> aliases) {
        if (metric == null || aliases.isEmpty()) {
            return false;
        }
        String normalizedId = normalizeMetricId(metric.getExternalId());
        if (normalizedId == null) {
            return false;
        }
        int idx = normalizedId.indexOf('_');
        if (idx <= 0 || idx >= normalizedId.length() - 1) {
            return false;
        }
        String suffix = normalizedId.substring(idx + 1);
        return matchesAlias(suffix, aliases);
    }

    private void applyMetricCategoriesFromReference(ProjectDTO referenceProject, List<MetricDTO> referenceMetrics,
            ProjectDTO newProject, List<MetricDTO> newMetrics) {
        if (newProject == null || referenceMetrics == null || newMetrics == null) {
            return;
        }

        MetricCategoryLookup lookup = buildMetricCategoryLookup(referenceProject, referenceMetrics);
        Set<String> newAliases = buildStudentAliases(newProject);

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
                } catch (Exception e) {
                    System.err.println("⚠ Error actualitzant la mètrica " + metric.getExternalId() + ": "
                            + e.getMessage());
                }
            } else if (desiredCategory == null) {
                noMatch++;
            }
        }
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

    private Map<String, String> buildMetricAliasCategoryLookup(List<MetricDTO> metrics, Set<String> aliases) {
        Map<String, String> lookup = new HashMap<>();
        if (metrics == null || aliases == null || aliases.isEmpty()) {
            return lookup;
        }
        for (MetricDTO metric : metrics) {
            if (metric == null || needsAliasCategory(metric.getCategoryName())) {
                continue;
            }
            String normalizedId = normalizeMetricId(metric.getExternalId());
            if (normalizedId == null) {
                continue;
            }
            String baseKey = buildMetricBaseKey(normalizedId, metric.getScope(), aliases);
            if (baseKey != null && !lookup.containsKey(baseKey)) {
                lookup.put(baseKey, metric.getCategoryName());
            }
        }
        return lookup;
    }

    private boolean needsAliasCategory(String category) {
        return category == null || category.isBlank() || "default".equalsIgnoreCase(category);
    }

    private String resolveCategoryFromAlias(MetricDTO metric, Set<String> aliases, Map<String, String> aliasLookup) {
        if (metric == null || aliases == null || aliases.isEmpty() || aliasLookup == null || aliasLookup.isEmpty()) {
            return null;
        }
        String normalizedId = normalizeMetricId(metric.getExternalId());
        if (normalizedId == null) {
            return null;
        }
        String baseKey = buildMetricBaseKey(normalizedId, metric.getScope(), aliases);
        if (baseKey == null) {
            return null;
        }
        String resolved = aliasLookup.get(baseKey);
        return resolved;
    }

    private void updateMetricCategory(MetricDTO metric, String category, String projectExternalId) {
        if (metric == null || metric.getId() == null || category == null || projectExternalId == null) {
            return;
        }
        metric.setCategoryName(category);
        ldService.editMetric(
                Long.parseLong(metric.getId()),
                null,
                null,
                category,
                metric.getScope(),
                projectExternalId);
    }

    private Set<String> buildStudentAliases(ProjectDTO project) {
        if (project == null) {
            return new HashSet<>();
        }
        return buildStudentAliases(project.getStudents());
    }

    private Set<String> buildStudentAliases(List<StudentDTO> students) {
        Set<String> aliases = new HashSet<>();
        if (students == null) {
            return aliases;
        }
        students.forEach(student -> aliases.addAll(extractAliasesForStudent(student)));
        return aliases;
    }

    private Set<String> extractAliasesForStudent(StudentDTO student) {
        Set<String> aliases = new HashSet<>();
        if (student == null) {
            return aliases;
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
