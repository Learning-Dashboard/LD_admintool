package com.upc.ld_admintool.domain.services;

import org.springframework.beans.factory.annotation.Autowired;
import com.upc.ld_admintool.rest.DTO.MetricDTO;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    @Autowired
    private LDService ldService;

    public List<MetricDTO> getMetricsByProject(String projectId) {
        return ldService.getMetricsByProject(projectId);
    }

    public List<String> getMetricsCategoriesList() {
        return ldService.getMetricsCategoriesList();
    }

    public List<Map<String, Object>> getAllMetricsCategories() {
        return ldService.getAllMetricsCategories();
    }

    public void editMetric(Long id, String threshold, String url, String categoryName, String scope, String project) {
        ldService.editMetric(id, threshold, url, categoryName, scope, project);
    }

    public void importMetrics() {
        ldService.importMetrics();
    }

}