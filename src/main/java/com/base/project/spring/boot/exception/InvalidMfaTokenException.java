package com.base.project.spring.boot.exception;

public class InvalidMfaTokenException extends RuntimeException {

    public InvalidMfaTokenException(String message) {
        super(message);
    }

}
