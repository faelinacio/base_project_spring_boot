package com.base.project.spring.boot.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String email) {
        super("Email '" + email + "' has not been verified yet");
    }

}
