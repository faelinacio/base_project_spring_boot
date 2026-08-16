package com.base.project.spring.boot.exception;

import org.springframework.http.HttpStatus;

public class InvalidTotpCodeException extends ApiException {

    public InvalidTotpCodeException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or expired authentication code");
    }

}
