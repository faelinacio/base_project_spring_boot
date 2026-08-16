package com.base.project.spring.boot.exception;

import org.springframework.http.HttpStatus;

public class InvalidVerificationTokenException extends ApiException {

    public InvalidVerificationTokenException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

}
