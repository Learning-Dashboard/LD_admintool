package com.upc.ld_admintool.rest.controllers;

import com.upc.ld_admintool.domain.services.MetricsService;
import com.upc.ld_admintool.rest.DTO.MetricDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @Autowired
    private MetricsService metricsService;

    @GetMapping
    public ResponseEntity<List<MetricDTO>> getMetricsByProject(@RequestParam("prj") String projectId) {
        System.out.println("Fetching metrics for project ID: " + projectId);
        return ResponseEntity.ok(metricsService.getMetricsByProject(projectId));
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> getMetricsCategoriesList() {
        return ResponseEntity.ok(metricsService.getMetricsCategoriesList());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getAllMetricsCategories() {
        return ResponseEntity.ok(metricsService.getAllMetricsCategories());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editMetric(@PathVariable Long id,
            @RequestParam(required = false) String threshold,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String scope,
            @RequestParam("prj") String project) {
        metricsService.editMetric(id, threshold, url, categoryName, scope, project);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/import")
    public ResponseEntity<Void> importMetrics() {
        metricsService.importMetrics();
        return ResponseEntity.ok().build();
    }
}