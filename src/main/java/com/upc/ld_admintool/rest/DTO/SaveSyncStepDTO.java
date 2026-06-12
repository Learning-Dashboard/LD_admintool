package com.upc.ld_admintool.rest.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveSyncStepDTO {

    private int order;
    private String name;
    private String detail;
    private String status; // SUCCESS, FAILED, SKIPPED
    private String error;
}
