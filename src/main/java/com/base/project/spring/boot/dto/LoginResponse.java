package com.base.project.spring.boot.dto;

/**
 * Result of {@code POST /api/auth/login}. When the account has TOTP enabled, {@code mfaRequired} is true and
 * {@code mfaToken} must be submitted together with a TOTP code to {@code POST /api/auth/login/totp} to obtain
 * {@code tokens}; otherwise {@code tokens} is already populated.
 */
public record LoginResponse(boolean mfaRequired, String mfaToken, AuthResponse tokens) {

    public static LoginResponse mfaRequired(String mfaToken) {
        return new LoginResponse(true, mfaToken, null);
    }

    public static LoginResponse authenticated(AuthResponse tokens) {
        return new LoginResponse(false, null, tokens);
    }

}
