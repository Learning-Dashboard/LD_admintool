package com.upc.ld_admintool.domain.services.credentials;

/** The login at a Taiga server failed, so no request can be authenticated. */
public class TaigaLoginException extends RuntimeException {

    public TaigaLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
