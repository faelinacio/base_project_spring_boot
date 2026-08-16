package com.base.project.spring.boot.exception;

import org.springframework.http.HttpStatus;

public class TotpAlreadyEnabledException extends ApiException {

    public TotpAlreadyEnabledException() {
        super(HttpStatus.CONFLICT,
                "TOTP is already enabled; disable it (with a valid code) before setting up a new secret");
    }

}
