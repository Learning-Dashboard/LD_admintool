package com.upc.ld_admintool.domain.services;
import com.upc.ld_admintool.rest.DTO.CategoryDTO;
import com.upc.ld_admintool.rest.DTO.IntervalDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class CategoriesService {

    @Autowired
    private LDService ldService;


    ////// METRICS /////////
    public void importarCategoriesMetriques(List<CategoryDTO> categories) {
        ldService.importarCategoriesMetriques(categories);
    }

    ////// FACTORS /////////
    public void importarCategoriesFactors(List<CategoryDTO> categories) {
        ldService.importarCategoriesFactors(categories);
    }

    ////// STRATEGIC INDICATORS /////////
    public void importarCategoriesStrategicIndicators(List<IntervalDTO> categories) {
        ldService.importarCategoriesStrategicIndicators(categories);
    }
}
