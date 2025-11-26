package com.upc.ld_admintool.rest.controllers;
import com.upc.ld_admintool.domain.services.CategoriesService;
import com.upc.ld_admintool.rest.DTO.CategoryDTO;
import com.upc.ld_admintool.rest.DTO.IntervalDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/categories")
public class CategoriesController {
    @Autowired
    private CategoriesService categoriesService;


    @PostMapping("/metrics")
    public ResponseEntity<?> importarCategoriesMetriques(@RequestBody List<CategoryDTO> categories) {
        categoriesService.importarCategoriesMetriques(categories);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/factors")
    public ResponseEntity<?> importarCategoriesFactors(@RequestBody List<CategoryDTO> categories) {
        categoriesService.importarCategoriesFactors(categories);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/strategicIndicators")
    public ResponseEntity<?> importarCategoriesStrategic(@RequestBody List<IntervalDTO> intervals) {
        categoriesService.importarCategoriesStrategicIndicators(intervals);
        return ResponseEntity.ok().build();
    }
}
