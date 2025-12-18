package com.upc.ld_admintool.rest.controllers;

import com.upc.ld_admintool.domain.services.WizardService;
import com.upc.ld_admintool.rest.DTO.WizardStatusDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wizard")
public class WizardController {

    @Autowired
    private WizardService wizardService;

    @GetMapping("/status")
    public ResponseEntity<WizardStatusDTO> getStatus() {
        return ResponseEntity.ok(wizardService.getWizardStatus());
    }
}
