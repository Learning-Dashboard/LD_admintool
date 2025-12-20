package com.upc.ld_admintool.domain.services.validation;

import com.upc.ld_admintool.domain.utils.DataSource;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.ProjectIdentityDTO;
import com.upc.ld_admintool.rest.DTO.StudentDTO;
import com.upc.ld_admintool.rest.DTO.StudentIdentityDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectValidationService {

    @Autowired
    private GitHubValidationService githubValidationService;

    @Autowired
    private TaigaValidationService taigaValidationService;

    /**
     * Valida un projecte complet amb els seus estudiants.
     * Utilitza el token de GitHub específic del projecte si està disponible.
     */
    public ValidationResult validateProject(ProjectDTO project) {
        ValidationResult result = new ValidationResult(true);

        System.out.println("🔍 Validant projecte: " + project.getName());

        // Validar identitats del projecte
        if (project.getIdentities() == null || project.getIdentities().isEmpty()) {
            result.addError("El projecte '" + project.getName() + "' no té identitats (GitHub/Taiga) definides");
            return result;
        }

        // Obtenir el token de GitHub del projecte (pot ser null)
        String projectGithubToken = project.getGithubToken();

        // Validar GitHub (com ho feia la Nora)
        ProjectIdentityDTO githubIdentity = project.getIdentities().get(DataSource.GITHUB);
        if (githubIdentity != null && githubIdentity.getUrl() != null) {
            String org = extractGitHubOrg(githubIdentity.getUrl());
            if (org != null) {
                // Validar organització amb el token del projecte
                ValidationResult orgResult = githubValidationService.validateOrganization(org, projectGithubToken);
                result.addErrors(orgResult.getErrors());
                result.addWarnings(orgResult.getWarnings());

                // Validar estudiants dins l'organització amb el token del projecte
                if (project.getStudents() != null) {
                    List<String> githubUsernames = new ArrayList<>();
                    for (StudentDTO student : project.getStudents()) {
                        if (student.getIdentities() != null) {
                            StudentIdentityDTO studentGithub = student.getIdentities().get(DataSource.GITHUB);
                            if (studentGithub != null && studentGithub.getUsername() != null) {
                                githubUsernames.add(studentGithub.getUsername());
                            }
                        }
                    }

                    if (!githubUsernames.isEmpty()) {
                        ValidationResult usersResult = githubValidationService.validateUsersInOrganization(
                                org, githubUsernames, projectGithubToken);
                        result.addErrors(usersResult.getErrors());
                        result.addWarnings(usersResult.getWarnings());
                    }
                }
            } else {
                result.addError("Format de URL GitHub invàlid per al projecte '" + project.getName() + "': "
                        + githubIdentity.getUrl());
            }
        } else {
            result.addWarning("El projecte '" + project.getName() + "' no té URL de GitHub definida");
        }

        // Validar Taiga
        ProjectIdentityDTO taigaIdentity = project.getIdentities().get(DataSource.TAIGA);
        if (taigaIdentity != null && taigaIdentity.getUrl() != null) {
            String slug = extractTaigaSlug(taigaIdentity.getUrl());
            if (slug != null) {
                ValidationResult taigaResult = taigaValidationService.validateProjectBySlug(slug);
                result.getErrors().addAll(taigaResult.getErrors());
                result.getWarnings().addAll(taigaResult.getWarnings());

                // Validar estudiants dins el projecte Taiga
                if (project.getStudents() != null) {
                    List<String> taigaUsernames = new ArrayList<>();
                    for (StudentDTO student : project.getStudents()) {
                        if (student.getIdentities() != null) {
                            StudentIdentityDTO studentTaiga = student.getIdentities().get(DataSource.TAIGA);
                            if (studentTaiga != null && studentTaiga.getUsername() != null) {
                                taigaUsernames.add(studentTaiga.getUsername());
                            }
                        }
                    }

                    if (!taigaUsernames.isEmpty()) {
                        ValidationResult usersResult = taigaValidationService.validateUsersInProject(slug,
                                taigaUsernames);
                        result.addErrors(usersResult.getErrors());
                        result.addWarnings(usersResult.getWarnings());
                    }
                }
            } else {
                result.addError("Format de URL Taiga invàlid per al projecte '" + project.getName() + "': "
                        + taigaIdentity.getUrl());
            }
        } else {
            result.addWarning("El projecte '" + project.getName() + "' no té URL de Taiga definida");
        }

        if (result.hasErrors()) {
            result.setValid(false);
            System.out.println("❌ Projecte '" + project.getName() + "' té errors: " + result.getErrors());
        } else {
            System.out.println("✅ Projecte '" + project.getName() + "' és vàlid" +
                    (projectGithubToken != null && !projectGithubToken.isEmpty() ? " (amb token propi)" : ""));
        }

        return result;
    }

    /**
     * Valida múltiples projectes i retorna quins són vàlids i quins no
     */
    public Map<String, Object> validateProjectsWithDetails(List<ProjectDTO> projects) {
        Map<String, Object> response = new HashMap<>();
        List<ProjectDTO> validProjects = new ArrayList<>();
        List<Map<String, Object>> invalidProjects = new ArrayList<>();

        for (ProjectDTO project : projects) {
            ValidationResult projectResult = validateProject(project);

            if (projectResult.hasErrors()) {
                // Projecte invàlid
                Map<String, Object> invalidInfo = new HashMap<>();
                invalidInfo.put("project", project);
                invalidInfo.put("errors", projectResult.getErrors());
                invalidInfo.put("warnings", projectResult.getWarnings());
                invalidProjects.add(invalidInfo);
            } else {
                // Projecte vàlid
                validProjects.add(project);
            }
        }

        response.put("validProjects", validProjects);
        response.put("invalidProjects", invalidProjects);
        response.put("totalProjects", projects.size());
        response.put("validCount", validProjects.size());
        response.put("invalidCount", invalidProjects.size());

        return response;
    }

    /**
     * Valida un estudiant individualment.
     */
    public ValidationResult validateStudent(String githubUrl, String taigaUrl, String githubToken, StudentDTO student) {
        ValidationResult result = new ValidationResult(true);

        if (student == null || student.getIdentities() == null) {
            result.addError("L'estudiant no té identitats definides");
            result.setValid(false);
            return result;
        }

        // Validar GitHub
        if (githubUrl != null) {
            String org = extractGitHubOrg(githubUrl);
            if (org != null) {
                StudentIdentityDTO studentGithub = student.getIdentities().get(DataSource.GITHUB);
                if (studentGithub != null && studentGithub.getUsername() != null) {
                    List<String> usernames = List.of(studentGithub.getUsername());
                    ValidationResult usersResult = githubValidationService.validateUsersInOrganization(
                            org, usernames, githubToken);
                    result.addErrors(usersResult.getErrors());
                    result.addWarnings(usersResult.getWarnings());
                }
            }
        }

        // Validar Taiga
        if (taigaUrl != null) {
            String slug = extractTaigaSlug(taigaUrl);
            if (slug != null) {
                StudentIdentityDTO studentTaiga = student.getIdentities().get(DataSource.TAIGA);
                if (studentTaiga != null && studentTaiga.getUsername() != null) {
                    List<String> usernames = List.of(studentTaiga.getUsername());
                    ValidationResult usersResult = taigaValidationService.validateUsersInProject(slug, usernames);
                    result.addErrors(usersResult.getErrors());
                    result.addWarnings(usersResult.getWarnings());
                }
            }
        }

        if (result.hasErrors()) {
            result.setValid(false);
        }

        return result;
    }

    /**
     * Extreu el nom de l'organització d'una URL de GitHub
     * Format: https://github.com/organization (com ho feia la Nora)
     */
    private String extractGitHubOrg(String url) {
        try {
            // Eliminar .git i barres finals
            url = url.replace(".git", "").replaceAll("/$", "");

            // Dividir per /
            String[] parts = url.split("/");

            // Buscar "github.com" i agafar el següent element (l'organització)
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].contains("github.com") && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        } catch (Exception e) {
            System.err.println("Error extraient organització de GitHub: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extreu el slug d'una URL de Taiga
     * Format: https://tree.taiga.io/project/owner-slug/
     */
    private String extractTaigaSlug(String url) {
        try {
            if (url.contains("/project/")) {
                String[] parts = url.split("/project/");
                if (parts.length > 1) {
                    String slug = parts[1].replaceAll("/$", "");
                    return slug;
                }
            }
        } catch (Exception e) {
            System.err.println("Error extraient slug de Taiga: " + e.getMessage());
        }
        return null;
    }
}
