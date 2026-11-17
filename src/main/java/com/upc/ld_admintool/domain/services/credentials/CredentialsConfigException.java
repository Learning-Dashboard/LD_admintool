package com.upc.ld_admintool.domain.services.credentials;

/**
 * The credentials config cannot be read, or it does not yield usable
 * credentials for a subject. The message is meant to be shown to the person
 * who maintains the file.
 */
public class CredentialsConfigException extends RuntimeException {

    public CredentialsConfigException(String message) {
        super(message);
    }

    public CredentialsConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
