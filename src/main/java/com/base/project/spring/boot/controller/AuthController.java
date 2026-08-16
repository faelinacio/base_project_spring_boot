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
import com.base.project.spring.boot.usecase.LoginUseCase;
import com.base.project.spring.boot.usecase.LogoutUseCase;
import com.base.project.spring.boot.usecase.RefreshTokenUseCase;
import com.base.project.spring.boot.usecase.RegisterUserUseCase;
import com.base.project.spring.boot.usecase.ResendVerificationEmailUseCase;
import com.base.project.spring.boot.usecase.VerifyEmailUseCase;
import com.base.project.spring.boot.usecase.VerifyTotpLoginUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerUserUseCase.execute(request));
    }

    /** Returns either a full token pair, or (when the account has TOTP enabled) an mfaToken for {@code /login/totp}. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }

    @PostMapping("/login/totp")
    public ResponseEntity<AuthResponse> loginTotp(@Valid @RequestBody TotpLoginRequest request) {
        return ResponseEntity.ok(verifyTotpLoginUseCase.execute(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(refreshTokenUseCase.execute(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        logoutUseCase.execute(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailUseCase.execute(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        resendVerificationEmailUseCase.execute(request.email());
        return ResponseEntity.accepted().build();
    }

}
