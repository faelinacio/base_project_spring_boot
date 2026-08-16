package com.base.project.spring.boot.usecase;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.base.project.spring.boot.config.EmailVerificationProperties;
import com.base.project.spring.boot.domain.EmailVerificationToken;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.email.EmailSender;
import com.base.project.spring.boot.repository.EmailVerificationTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Shared by the register and resend-verification use cases: mints a fresh verification token for a user (invalidating
 * any earlier one) and emails the raw token as a link. {@code issueToken} and {@code sendVerificationEmail} are exposed
 * as two separate calls (rather than one method doing both) so callers commit the DB writes in their own short
 * transaction before performing the (potentially slow) email send — a self-invoking wrapper method here wouldn't work
 * for that: Spring's proxy-based {@code @Transactional} only intercepts calls made through the bean, not one method on
 * this class calling another internally.
 */
@Component
@RequiredArgsConstructor
class EmailVerificationIssuer {

    private final EmailVerificationTokenRepository tokenRepository;
    private final TokenHasher hasher;
    private final EmailSender emailSender;
    private final EmailVerificationProperties properties;

    @Transactional
    String issueToken(User user) {
        tokenRepository.invalidateAllByUserId(user.getId());

        String rawToken = generateRawToken();
        Instant expiresAt = Instant.now().plus(properties.tokenExpiration());
        tokenRepository.save(EmailVerificationToken.builder().userId(user.getId()).tokenHash(hasher.hash(rawToken))
                .expiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)).used(false).build());
        return rawToken;
    }

    void sendVerificationEmail(User user, String rawToken) {
        // Uses "&" instead of "?" when verificationBaseUrl already carries a query string, rather than
        // assuming it never does.
        String link = UriComponentsBuilder.fromUriString(properties.verificationBaseUrl()).queryParam("token", rawToken)
                .build().toUriString();
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
