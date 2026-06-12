package com.upc.ld_admintool.domain.services;

import com.upc.ld_admintool.rest.DTO.WizardStatusDTO;
import com.upc.ld_admintool.rest.DTO.ProjectDTO;
import com.upc.ld_admintool.rest.DTO.MetricDTO;
import com.upc.ld_admintool.rest.DTO.FactorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WizardService {

    @Autowired
    private LDService ldService;

    public WizardStatusDTO getWizardStatus() {
        boolean hasProjects = false;
        boolean hasData = false;
        boolean hasMetricsCategories = false;
        boolean hasFactorsCategories = false;
        boolean hasStrategicIndicatorCategories = false;

        boolean hasAssignments = false;
        try {
            // 1. Projects
            List<ProjectDTO> projects = ldService.getAllProjects();
            hasProjects = !projects.isEmpty();

            // 2. Categories
            List<Map<String, Object>> metricsCats = ldService.getAllMetricsCategories();
            hasMetricsCategories = (metricsCats != null && !metricsCats.isEmpty());

            List<Map<String, Object>> factorsCats = ldService.getAllFactorsCategories();
            hasFactorsCategories = (factorsCats != null && !factorsCats.isEmpty());

            List<Map<String, Object>> siCats = ldService.getAllStrategicIndicatorCategories();
            hasStrategicIndicatorCategories = (siCats != null && !siCats.isEmpty());

            // 3. Data
            if (hasProjects) {
                String externalId = projects.get(0).getExternalId();
                if (externalId != null) {
                    List<MetricDTO> metrics = ldService.getMetricsByProject(externalId);
                    List<FactorDTO> factors = ldService.getFactorsByProject(externalId);
                    hasData = !metrics.isEmpty() && !factors.isEmpty();
                }
            }

        } catch (Exception e) {
            System.err.println("Error calculating wizard status: " + e.getMessage());
        }

        return new WizardStatusDTO(hasProjects, hasData, hasMetricsCategories, hasFactorsCategories,
                hasStrategicIndicatorCategories);
    }
}
