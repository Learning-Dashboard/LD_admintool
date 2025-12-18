package com.upc.ld_admintool.rest.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WizardStatusDTO {
    private boolean hasProjects;
    private boolean hasData;
    private boolean hasMetricsCategories;
    private boolean hasFactorsCategories;
    private boolean hasStrategicIndicatorCategories;
}
