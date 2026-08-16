package com.base.project.spring.boot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.base.project.spring.boot.dto.TotpCodeRequest;
import com.base.project.spring.boot.dto.TotpSetupResponse;
import com.base.project.spring.boot.security.UserPrincipal;
import com.base.project.spring.boot.usecase.DisableTotpUseCase;
import com.base.project.spring.boot.usecase.EnableTotpUseCase;
import com.base.project.spring.boot.usecase.SetupTotpUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me/totp")
@RequiredArgsConstructor
public class TotpController {

    private final SetupTotpUseCase setupTotpUseCase;
    private final EnableTotpUseCase enableTotpUseCase;
    private final DisableTotpUseCase disableTotpUseCase;

    @PostMapping("/setup")
    public TotpSetupResponse setup(@AuthenticationPrincipal UserPrincipal principal) {
        return setupTotpUseCase.execute(principal.getId());
    }

    @PostMapping("/enable")
    public ResponseEntity<Void> enable(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TotpCodeRequest request) {
        enableTotpUseCase.execute(principal.getId(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TotpCodeRequest request) {
        disableTotpUseCase.execute(principal.getId(), request.code());
        return ResponseEntity.noContent().build();
    }

}
