package com.base.project.spring.boot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.base.project.spring.boot.dto.TotpCodeRequest;
import com.base.project.spring.boot.dto.TotpSetupResponse;
import com.base.project.spring.boot.exception.ErrorResponse;
import com.base.project.spring.boot.security.UserPrincipal;
import com.base.project.spring.boot.usecase.DisableTotpUseCase;
import com.base.project.spring.boot.usecase.EnableTotpUseCase;
import com.base.project.spring.boot.usecase.SetupTotpUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "TOTP 2FA", description = "Time-based one-time-password two-factor authentication enrollment for the current user.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users/me/totp")
@RequiredArgsConstructor
public class TotpController {

    private final SetupTotpUseCase setupTotpUseCase;
    private final EnableTotpUseCase enableTotpUseCase;
    private final DisableTotpUseCase disableTotpUseCase;

    @Operation(summary = "Start TOTP setup", description = "Generates a new secret and QR code. TOTP isn't enabled until a valid code is submitted to "
            + "POST /enable.")
    @ApiResponse(responseCode = "200", description = "Secret and QR code generated")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "409", description = "TOTP is already enabled on this account", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/setup")
    public TotpSetupResponse setup(@AuthenticationPrincipal UserPrincipal principal) {
        return setupTotpUseCase.execute(principal.getId());
    }

    @Operation(summary = "Enable TOTP", description = "Confirms setup by validating a code generated from the pending secret.")
    @ApiResponse(responseCode = "204", description = "TOTP enabled")
    @ApiResponse(responseCode = "400", description = "Validation failed, or no pending TOTP setup", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token, or invalid code", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/enable")
    public ResponseEntity<Void> enable(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TotpCodeRequest request) {
        enableTotpUseCase.execute(principal.getId(), request.code());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Disable TOTP", description = "Requires a currently valid TOTP code as confirmation.")
    @ApiResponse(responseCode = "204", description = "TOTP disabled")
    @ApiResponse(responseCode = "400", description = "Validation failed, or TOTP is not enabled", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token, or invalid code", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TotpCodeRequest request) {
        disableTotpUseCase.execute(principal.getId(), request.code());
        return ResponseEntity.noContent().build();
    }

}
