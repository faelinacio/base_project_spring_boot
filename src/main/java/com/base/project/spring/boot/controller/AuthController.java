package com.base.project.spring.boot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.LoginRequest;
import com.base.project.spring.boot.dto.LoginResponse;
import com.base.project.spring.boot.dto.RefreshRequest;
import com.base.project.spring.boot.dto.RegisterRequest;
import com.base.project.spring.boot.dto.ResendVerificationRequest;
import com.base.project.spring.boot.dto.TotpLoginRequest;
import com.base.project.spring.boot.dto.VerifyEmailRequest;
import com.base.project.spring.boot.exception.ErrorResponse;
import com.base.project.spring.boot.usecase.LoginUseCase;
import com.base.project.spring.boot.usecase.LogoutUseCase;
import com.base.project.spring.boot.usecase.RefreshTokenUseCase;
import com.base.project.spring.boot.usecase.RegisterUserUseCase;
import com.base.project.spring.boot.usecase.ResendVerificationEmailUseCase;
import com.base.project.spring.boot.usecase.VerifyEmailUseCase;
import com.base.project.spring.boot.usecase.VerifyTotpLoginUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "Registration, login, token refresh/logout and email verification. No authentication required.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationEmailUseCase resendVerificationEmailUseCase;
    private final VerifyTotpLoginUseCase verifyTotpLoginUseCase;

    @Operation(summary = "Register a new account", description = "Creates the user and sends an email verification link. The returned tokens can be used immediately, "
            + "even before the email is verified.")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerUserUseCase.execute(request));
    }

    @Operation(summary = "Log in with email and password", description = "Returns either a full token pair, or (when the account has TOTP enabled) an mfaToken for "
            + "POST /api/auth/login/totp.")
    @ApiResponse(responseCode = "200", description = "Authenticated, or MFA challenge issued")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Account is disabled, or email not yet verified", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }

    @Operation(summary = "Complete a TOTP-challenged login", description = "Exchanges the mfaToken from POST /api/auth/login plus a current TOTP code for a full token pair.")
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid or expired mfaToken, or invalid TOTP code", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/login/totp")
    public ResponseEntity<AuthResponse> loginTotp(@Valid @RequestBody TotpLoginRequest request) {
        return ResponseEntity.ok(verifyTotpLoginUseCase.execute(request));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair", description = "Rotates the refresh token: the one submitted is invalidated and a new one is returned.")
    @ApiResponse(responseCode = "200", description = "New token pair issued")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid, expired or already-used refresh token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(refreshTokenUseCase.execute(request.refreshToken()));
    }

    @Operation(summary = "Log out", description = "Invalidates the given refresh token.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        logoutUseCase.execute(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verify an email address", description = "Consumes the token sent in the verification email.")
    @ApiResponse(responseCode = "204", description = "Email verified")
    @ApiResponse(responseCode = "400", description = "Validation failed, or verification token is invalid, expired or already used", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailUseCase.execute(request.token());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resend the email verification link", description = "Always returns 202, whether or not the email is registered, to avoid leaking account existence.")
    @ApiResponse(responseCode = "202", description = "Request accepted")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        resendVerificationEmailUseCase.execute(request.email());
        return ResponseEntity.accepted().build();
    }

}
