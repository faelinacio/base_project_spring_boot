package com.base.project.spring.boot.exception;

public class TotpAlreadyEnabledException extends RuntimeException {

    public TotpAlreadyEnabledException() {
        super("TOTP is already enabled; disable it (with a valid code) before setting up a new secret");
    }

}
