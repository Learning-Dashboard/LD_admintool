package com.upc.ld_admintool.domain.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.upc.ld_admintool.rest.DTO.*;
import com.upc.ld_admintool.domain.utils.DataSource;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class LDService {

    static final String LD_API_KEY_HEADER = "X-LD-API-Key";

    @Value("${ld.api.url}")
    private String ldApiUrl; // http://localhost:8888/api

    @Value("${ld.api.key:${LD_API_KEY:}}")
    private String ldApiKey;

    private final RestTemplate restTemplate;

    public LDService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(120_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @PostConstruct
    void configureApiKeyInterceptor() {
        if (ldApiKey == null || ldApiKey.trim().isEmpty()) {
            throw new IllegalStateException("LD_API_KEY must be configured for Learning Dashboard API calls");
        }
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
        interceptors.removeIf(interceptor -> interceptor instanceof LdApiKeyInterceptor);
        interceptors.add(new LdApiKeyInterceptor(ldApiKey.trim()));
        restTemplate.setInterceptors(interceptors);
    }

    // -------------------------------
    // Crear projecte al Learning Dashboard
    // -------------------------------
    public Long createProject(ProjectDTO project) {
        try {
            String url = ldApiUrl + "/projects";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ProjectDTO> request = new HttpEntity<>(project, headers);
            ResponseEntity<ProjectDTO> response = restTemplate.postForEntity(url, request, ProjectDTO.class);

            return Objects.requireNonNull(response.getBody()).getId();
        } catch (HttpClientErrorException e) {
            System.err.println("Error creating project: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------
    // Crear estudiant dins un projecte
    // -------------------------------
    public void createStudent(Long projectId, StudentDTO student) {
        String url = ldApiUrl + "/projects/" + projectId + "/students";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<StudentDTO> request = new HttpEntity<>(student, headers);
        try {
            restTemplate.postForEntity(url, request, StudentDTO.class);
        } catch (HttpClientErrorException e) {
            System.err.println("Error creating student: " + e.getMessage());
        }
    }

    // -------------------------------
    // Obtenir tots els projectes
    // -------------------------------
    public List<ProjectDTO> getAllProjects() {
        String url = ldApiUrl + "/projects";
        ResponseEntity<ProjectDTO[]> response = restTemplate.getForEntity(url, ProjectDTO[].class);
        return Arrays.asList(response.getBody());
    }

    // -------------------------------
    // Obtenir projecte per id
    // -------------------------------
    public ProjectDTO getProjectById(Long id) {
        String url = ldApiUrl + "/projects/" + id;
        try {
            ResponseEntity<ProjectDTO> response = restTemplate.getForEntity(url, ProjectDTO.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching project by ID: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------
    // Actualitzar projecte
    // -------------------------------
    public void updateProject(Long id, ProjectDTO project) {
        String url = ldApiUrl + "/projects/" + id;
        ObjectMapper mapper = new ObjectMapper();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            Map<String, Object> updateData = new HashMap<>();

            // Mapping als camps esperats pel backend LD:
            updateData.put("external_id", project.getExternalId());
            updateData.put("name", project.getName());
            updateData.put("description", project.getDescription());
            updateData.put("backlog_id", project.getBacklogId());
            // Atenció: adapta mapping de identities al format correcte
            // S'ha d'enviar Map<DataSource, String>. Probablement necessites crear un Map a
            // partir de project.getIdentities()
            Map<DataSource, String> identities = new HashMap<>();
            if (project.getIdentities() != null) {
                project.getIdentities().forEach((ds, pid) -> {
                    if (pid != null && pid.getUrl() != null)
                        identities.put(ds, pid.getUrl());
                });
            }
            updateData.put("identities", identities);
            updateData.put("global", project.getIsGlobal());
            if (project.getStudents() != null) {
                updateData.put("students", project.getStudents());
            }
            System.out.println("Update data prepared: " + updateData);

            HttpHeaders jsonHeaders = new HttpHeaders();
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> jsonPart = new HttpEntity<>(mapper.writeValueAsString(updateData), jsonHeaders);
            body.add("data", jsonPart);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serialitzant updateData a JSON!", e);
        } catch (HttpClientErrorException e) {
            System.err.println("Error updating project: " + e.getMessage());
        }
    }

    // -------------------------------
    // Eliminar estudiant
    // -------------------------------
    public void deleteStudent(Long studentId) {
        String url = ldApiUrl + "/metrics/students/" + studentId;
        try {
            restTemplate.delete(url);
        } catch (HttpClientErrorException e) {
            System.err.println("Error deleting student: " + e.getMessage());
        }
    }

    // -------------------------------
    // Eliminar projecte
    // -------------------------------
    public void deleteProject(Long id) {
        String url = ldApiUrl + "/projects/" + id;
        try {
            restTemplate.delete(url);
        } catch (HttpClientErrorException e) {
            System.err.println("Error deleting project: " + e.getMessage());
        }
    }

    // -------------------------------
    // Obtenir mètriques d'un projecte
    // -------------------------------
    public List<MetricDTO> getMetricsByProject(String projectId) {

        String url = ldApiUrl + "/metrics?prj=" + projectId;
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            List<Map<String, Object>> data = response.getBody();
            List<MetricDTO> metrics = new ArrayList<>();
            for (Map<String, Object> m : data) {
                metrics.add(new MetricDTO(
                        String.valueOf(m.get("id")),
                        (String) m.get("externalId"),
                        (String) m.get("name"),
                        (String) m.get("description"),
                        (String) m.get("categoryName"),
                        (String) m.get("scope")));
            }
            return metrics;
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching metrics: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // -------------------------------
    // Obtenir categories de mètriques
    // -------------------------------
    public List<Map<String, Object>> getAllMetricsCategories() {
        String url = ldApiUrl + "/metrics/categories";
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }

    // -------------------------------
    // Obtenir llista de categories de mètriques
    // -------------------------------
    public List<String> getMetricsCategoriesList() {
        String url = ldApiUrl + "/metrics/list";
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }

    // -------------------------------
    // Editar mètrica
    // -------------------------------
    public void editMetric(Long id, String threshold, String url, String categoryName, String scope, String project) {
        String apiUrl = ldApiUrl + "/metrics/" + id + "?prj=" + URLEncoder.encode(project, StandardCharsets.UTF_8);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("threshold", threshold != null ? threshold : "");
        formData.add("url", url != null ? url : "");
        formData.add("categoryName", categoryName != null ? categoryName : "");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            restTemplate.exchange(apiUrl, HttpMethod.PUT, request, Void.class);
        } catch (HttpClientErrorException e) {
            System.err.println("   ❌ Error editing metric: " + e.getMessage());
            throw e;
        }
    }

    // -------------------------------
    // Importar categories de mètriques
    // -------------------------------
    public void importarCategoriesMetriques(List<CategoryDTO> categories) {
        System.out.println("Importing metric categories: " + categories);
        for (CategoryDTO cat : categories) {
            String url = ldApiUrl + "/metrics/categories?name=" + cat.getCategory()
                    + (cat.getPatternGroup() != null ? "&patternGroup=" + cat.getPatternGroup() : "");
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<List<Map<String, String>>> request = new HttpEntity<>(cat.getInterval(), headers);
                restTemplate.postForEntity(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error important categoria " + cat.getCategory() + ": " + e.getMessage());
            }
        }
    }

    // -------------------------------
    // Obtenir factors d'un projecte
    // -------------------------------
    public List<FactorDTO> getFactorsByProject(String projectId) {
        String url = ldApiUrl + "/qualityFactors?prj=" + projectId;
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        List<Map<String, Object>> data = response.getBody();

        List<FactorDTO> factors = new ArrayList<>();
        for (Map<String, Object> f : data) {
            factors.add(new FactorDTO(
                    String.valueOf(f.get("id")),
                    (String) f.get("externalId"),
                    (String) f.get("name"),
                    (String) f.get("description"),
                    (String) f.get("categoryName"),
                    f.get("threshold") != null ? String.valueOf(f.get("threshold")) : null,
                    (String) f.get("type"),
                    (List<String>) f.get("metrics"),
                    (List<String>) f.get("metricsWeights")));
        }
        return factors;
    }

    // -------------------------------
    // Obtenir llista de categories de factors
    // -------------------------------
    public List<String> getFactorsCategoriesList() {
        String url = ldApiUrl + "/factors/list";
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }

    // -------------------------------
    // Obtenir categories de factors
    // -------------------------------
    public List<Map<String, Object>> getAllFactorsCategories() {
        String url = ldApiUrl + "/factors/categories";
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }

    // -------------------------------
    // Editar factor
    // -------------------------------
    public void updateFactorCategory(Long id, String category, String project) {
        String apiUrl = ldApiUrl + "/qualityFactors/" + id +
                "/category?prj=" + URLEncoder.encode(project, StandardCharsets.UTF_8);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("category", category);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        restTemplate.exchange(apiUrl, HttpMethod.PUT, request, Void.class);
    }

    // -------------------------------
    // Importar categories de factors
    // -------------------------------
    public void importarCategoriesFactors(List<CategoryDTO> categories) {
        System.out.println("Importing factor categories: " + categories);
        for (CategoryDTO cat : categories) {
            String url = ldApiUrl + "/factors/categories?name=" + cat.getCategory()
                    + (cat.getPatternGroup() != null ? "&patternGroup=" + cat.getPatternGroup() : "");
            System.out.println("Importing category to URL: " + url);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<List<Map<String, String>>> request = new HttpEntity<>(cat.getInterval(), headers);
                restTemplate.postForEntity(url, request, Void.class);
            } catch (Exception e) {
                System.err.println("Error important categoria " + cat.getCategory() + ": " + e.getMessage());
            }
        }
    }

    // -------------------------------
    // Obtenir categories d'indicadors estratègics
    // -------------------------------
    public List<Map<String, Object>> getAllStrategicIndicatorCategories() {
        String url = ldApiUrl + "/strategicIndicators/categories";
        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
        return response.getBody();
    }

    // -------------------------------
    // Importar categories d'indicadors estratègics
    // -------------------------------
    public void importarCategoriesStrategicIndicators(List<IntervalDTO> dtos) {
        try {
            List<Map<String, String>> categories = dtos.stream()
                    .map(dto -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("name", dto.getName());
                        map.put("color", dto.getColor());
                        return map;
                    })
                    .collect(Collectors.toList());
            System.out.println("Importing strategic indicator categories: " + categories);
            String url = ldApiUrl + "/strategicIndicators/categories";
            restTemplate.postForEntity(url, categories, Void.class);
        } catch (HttpClientErrorException e) {
            System.err.println("Error importing strategic indicator categories: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error importing strategic indicator categories: " + e.getMessage());
        }
    }

    // -------------------------------
    // Importar mètriques (cridar LD API /api/metrics/import)
    // -------------------------------
    public void importMetrics() {
        String url = ldApiUrl + "/metrics/import";
        try {
            restTemplate.getForEntity(url, Void.class);
            System.out.println("Metrics imported successfully");
        } catch (HttpClientErrorException e) {
            System.err.println("Error importing metrics: " + e.getMessage());
            throw e;
        }
    }

    // -------------------------------
    // Importar quality factors (cridar LD API /api/qualityFactors/import)
    // -------------------------------
    public void importQualityFactors() {
        String url = ldApiUrl + "/qualityFactors/import";
        try {
            restTemplate.getForEntity(url, Void.class);
            System.out.println("Quality Factors imported successfully");
        } catch (HttpClientErrorException e) {
            System.err.println("Error importing quality factors: " + e.getMessage());
            throw e;
        }
    }

    // -------------------------------
    // Fetch strategic indicators (cridar LD API /api/strategicIndicators/fetch)
    // -------------------------------
    public void fetchStrategicIndicators() {
        String url = ldApiUrl + "/strategicIndicators/fetch";
        try {
            restTemplate.getForEntity(url, Void.class);
            System.out.println("Strategic Indicators fetched successfully");
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching strategic indicators: " + e.getMessage());
            throw e;
        }
    }

    private static class LdApiKeyInterceptor implements ClientHttpRequestInterceptor {
        private final String apiKey;

        private LdApiKeyInterceptor(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request,
                                            byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set(LD_API_KEY_HEADER, apiKey);
            return execution.execute(request, body);
        }
    }

}
