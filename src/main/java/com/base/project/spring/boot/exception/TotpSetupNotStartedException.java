package com.base.project.spring.boot.exception;

public class TotpSetupNotStartedException extends RuntimeException {

    public TotpSetupNotStartedException() {
        super("TOTP is not set up for this account");
    }

}
