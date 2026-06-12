package com.upc.ld_admintool.rest.controllers;

import com.upc.ld_admintool.domain.services.StrategicIndicatorsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/strategicIndicators")
public class StrategicIndicatorsController {

    @Autowired
    private StrategicIndicatorsService strategicIndicatorsService;

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getAllStrategicIndicatorCategories() {
        return ResponseEntity.ok(strategicIndicatorsService.getAllStrategicIndicatorCategories());
    }

    @GetMapping("/fetch")
    public ResponseEntity<Void> fetchStrategicIndicators() {
        strategicIndicatorsService.fetchStrategicIndicators();
        return ResponseEntity.ok().build();
    }
}
