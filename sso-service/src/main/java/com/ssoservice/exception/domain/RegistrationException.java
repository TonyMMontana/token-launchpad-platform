package com.ssoservice.exception.domain;

public class RegistrationException extends RuntimeException {
    public RegistrationException() {
        super();
    }

    public RegistrationException(String message) {
        super(message);
    }
}
