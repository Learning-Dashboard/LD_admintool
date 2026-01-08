package com.upc.ld_admintool.rest.DTO;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class SaveSyncResponseDTO {

    private boolean success = true;
    private int finalTeamSize;
    private List<SaveSyncStepDTO> steps = new ArrayList<>();

    public void addSuccessStep(int order, String name, String detail) {
        steps.add(new SaveSyncStepDTO(order, name, detail, "SUCCESS", null));
    }

    public void addFailureStep(int order, String name, String detail, String error) {
        steps.add(new SaveSyncStepDTO(order, name, detail, "FAILED", error));
        this.success = false;
    }

    public void addSkippedStep(int order, String name, String detail) {
        steps.add(new SaveSyncStepDTO(order, name, detail, "SKIPPED", null));
    }
}
