package com.base.project.spring.boot.exception;

import org.springframework.http.HttpStatus;

public class InvalidMfaTokenException extends ApiException {

    public InvalidMfaTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

}
