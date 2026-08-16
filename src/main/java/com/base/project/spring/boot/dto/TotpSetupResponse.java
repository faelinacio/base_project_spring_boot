package com.base.project.spring.boot.dto;

/**
 * {@code qrCodeImage} is a ready-to-render {@code data:image/png;base64,...} URI; {@code secret} lets users type it in.
 */
public record TotpSetupResponse(String secret, String qrCodeImage) {
}
