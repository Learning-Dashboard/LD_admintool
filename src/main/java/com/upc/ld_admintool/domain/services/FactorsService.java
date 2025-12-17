package com.upc.ld_admintool.domain.services;

import org.springframework.beans.factory.annotation.Autowired;
import com.upc.ld_admintool.rest.DTO.FactorDTO;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class FactorsService {

    @Autowired
    private LDService ldService;

    public List<FactorDTO> getFactorsByProject(String projectId) {
        return ldService.getFactorsByProject(projectId);
    }

    public List<String> getFactorsCategoriesList() {
        return ldService.getFactorsCategoriesList();
    }

    public List<Map<String, Object>> getAllFactorsCategories() {
        return ldService.getAllFactorsCategories();
    }

    public void updateFactorCategory(Long id, String category, String project) {
        ldService.updateFactorCategory(id, category, project);
    }

    public void importQualityFactors() {
        ldService.importQualityFactors();
    }
}