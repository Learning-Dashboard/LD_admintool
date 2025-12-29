package com.upc.ld_admintool.rest.DTO;

import java.util.ArrayList;
import java.util.List;

public class SaveSyncResponseDTO {

    private boolean success = true;
    private int finalTeamSize;
    private List<SaveSyncStepDTO> steps = new ArrayList<>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getFinalTeamSize() {
        return finalTeamSize;
    }

    public void setFinalTeamSize(int finalTeamSize) {
        this.finalTeamSize = finalTeamSize;
    }

    public List<SaveSyncStepDTO> getSteps() {
        return steps;
    }

    public void setSteps(List<SaveSyncStepDTO> steps) {
        this.steps = steps;
    }

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
