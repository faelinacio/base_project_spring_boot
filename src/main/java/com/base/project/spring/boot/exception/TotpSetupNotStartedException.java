package com.base.project.spring.boot.exception;

import org.springframework.http.HttpStatus;

public class TotpSetupNotStartedException extends ApiException {

    public TotpSetupNotStartedException() {
        super(HttpStatus.BAD_REQUEST, "TOTP is not set up for this account");
    }

}
