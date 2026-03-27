package com.upc.ld_admintool.domain.services.validation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;

    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public void addErrors(List<String> errors) {
        if (errors != null && !errors.isEmpty()) {
            this.errors.addAll(errors);
            this.valid = false;
        }
    }
    
    public void addWarnings(List<String> warnings) {
        if (warnings != null) {
            this.warnings.addAll(warnings);
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
