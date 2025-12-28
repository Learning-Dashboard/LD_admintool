package com.upc.ld_admintool.domain.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class StrategicIndicatorsService {

    @Autowired
    private LDService ldService;

    @Autowired
    private ProjectService projectService;

    public List<Map<String, Object>> getAllStrategicIndicatorCategories() {
        return ldService.getAllStrategicIndicatorCategories();
    }

    public void fetchStrategicIndicators() {
        ldService.fetchStrategicIndicators();
        projectService.synchronizeCategoriesAfterDataImport();
    }
}
