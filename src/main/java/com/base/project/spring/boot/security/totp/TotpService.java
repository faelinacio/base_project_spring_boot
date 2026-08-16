package com.base.project.spring.boot.security.totp;

import java.util.Base64;

import org.springframework.stereotype.Service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

/**
 * Generates and verifies TOTP (RFC 6238) secrets/codes for authenticator apps (Google Authenticator, Authy, etc.), and
 * renders the enrollment QR code. Free/offline: no SMS gateway or third-party service involved.
 */
@Service
public class TotpService {

    private static final String ISSUER = "BaseProjectSpringBoot";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public boolean verifyCode(String secret, String code) {
        return code != null && codeVerifier.isValidCode(secret, code);
    }

    /** Data URI (base64 PNG) of the QR code an authenticator app scans to enroll {@code secret} for {@code email}. */
    public String generateQrCodeDataUri(String secret, String email) {
        QrData data = new QrData.Builder().label(email).secret(secret).issuer(ISSUER).algorithm(HashingAlgorithm.SHA1)
                .digits(6).period(30).build();
        try {
            byte[] png = qrGenerator.generate(data);
            return "data:" + qrGenerator.getImageMimeType() + ";base64," + Base64.getEncoder().encodeToString(png);
        } catch (QrGenerationException e) {
            throw new IllegalStateException("Failed to generate TOTP QR code", e);
        }
    }

}
