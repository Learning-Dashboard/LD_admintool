package com.upc.ld_admintool.rest.controllers;
import com.upc.ld_admintool.domain.services.FactorsService;
import com.upc.ld_admintool.rest.DTO.FactorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/factors")
public class FactorsController {

    @Autowired
    private FactorsService factorsService;

    @GetMapping
    public ResponseEntity<List<FactorDTO>> getFactorsByProject(@RequestParam("prj") String projectId) {
        return ResponseEntity.ok(factorsService.getFactorsByProject(projectId));
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> getFactorsCategoriesList() {
        return ResponseEntity.ok(factorsService.getFactorsCategoriesList());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getAllFactorsCategories() {
        return ResponseEntity.ok(factorsService.getAllFactorsCategories());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editFactor(@PathVariable Long id,
                        @RequestParam(required = false) String threshold,
                        @RequestParam(required = false) String url,
                        @RequestParam(required = false) String categoryName,
                        @RequestParam("prj") String project) {
        factorsService.editFactor(id, threshold, url, categoryName, project);
        return ResponseEntity.ok().build();
    }
}