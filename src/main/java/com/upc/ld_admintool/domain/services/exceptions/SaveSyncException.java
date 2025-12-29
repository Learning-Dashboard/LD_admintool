package com.upc.ld_admintool.domain.services.exceptions;

import com.upc.ld_admintool.rest.DTO.SaveSyncResponseDTO;

public class SaveSyncException extends RuntimeException {

    private final SaveSyncResponseDTO response;

    public SaveSyncException(String message, SaveSyncResponseDTO response) {
        super(message);
        this.response = response;
    }

    public SaveSyncException(String message, Throwable cause, SaveSyncResponseDTO response) {
        super(message, cause);
        this.response = response;
    }

    public SaveSyncResponseDTO getResponse() {
        return response;
    }
}
