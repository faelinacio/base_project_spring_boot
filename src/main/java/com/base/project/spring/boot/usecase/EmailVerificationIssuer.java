package com.base.project.spring.boot.usecase;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.base.project.spring.boot.config.EmailVerificationProperties;
import com.base.project.spring.boot.domain.EmailVerificationToken;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.email.EmailSender;
import com.base.project.spring.boot.repository.EmailVerificationTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Shared by the register and resend-verification use cases: mints a fresh verification token for a user (invalidating
 * any earlier one), persists it (hashed), and emails the raw token as a link.
 */
@Component
@RequiredArgsConstructor
class EmailVerificationIssuer {

    private final EmailVerificationTokenRepository tokenRepository;
    private final TokenHasher hasher;
    private final EmailSender emailSender;
    private final EmailVerificationProperties properties;

    void issueFor(User user) {
        tokenRepository.invalidateAllByUserId(user.getId());

        String rawToken = generateRawToken();
        Instant expiresAt = Instant.now().plus(properties.tokenExpiration());
        tokenRepository.save(EmailVerificationToken.builder().userId(user.getId()).tokenHash(hasher.hash(rawToken))
                .expiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)).used(false).build());

        String link = properties.verificationBaseUrl() + "?token=" + rawToken;
        emailSender.send(user.getEmail(), "Confirme seu e-mail",
                "Ola " + user.getName() + ",\n\nConfirme seu e-mail acessando o link abaixo:\n" + link
                        + "\n\nEste link expira em " + properties.tokenExpiration().toHours()
                        + " horas. Se voce nao criou esta conta, ignore este e-mail.");
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
