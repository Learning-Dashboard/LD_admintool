package com.upc.ld_admintool.rest.DTO;

public class SaveSyncStepDTO {

    private int order;
    private String name;
    private String detail;
    private String status; // SUCCESS, FAILED, SKIPPED
    private String error;

    public SaveSyncStepDTO() {
    }

    public SaveSyncStepDTO(int order, String name, String detail, String status, String error) {
        this.order = order;
        this.name = name;
        this.detail = detail;
        this.status = status;
        this.error = error;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
