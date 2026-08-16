package com.base.project.spring.boot.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Base for exceptions that map to a single fixed HTTP status + message, so GlobalExceptionHandler needs one generic
 * {@code @ExceptionHandler(ApiException.class)} instead of a near-identical method per exception type.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

}
