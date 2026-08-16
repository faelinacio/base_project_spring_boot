package com.base.project.spring.boot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * {@code qrCodeImage} is a ready-to-render {@code data:image/png;base64,...} URI; {@code secret} lets users type it in.
 */
public record TotpSetupResponse(@Schema(description = "Base32 TOTP secret, for manual entry.") String secret,
        @Schema(description = "Ready-to-render data:image/png;base64,... URI containing the QR code.") String qrCodeImage) {
}
