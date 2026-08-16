package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.base.project.spring.boot.config.EmailVerificationProperties;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.email.EmailSender;
import com.base.project.spring.boot.repository.EmailVerificationTokenRepository;

@ExtendWith(MockitoExtension.class)
class EmailVerificationIssuerTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    private final TokenHasher hasher = new TokenHasher();

    @Mock
    private EmailSender emailSender;

    private EmailVerificationIssuer issuer;

    @BeforeEach
    void setUp() {
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendVerificationEmail_whenBaseUrlHasNoQueryString_appendsTokenWithQuestionMark() {
        issuer = new EmailVerificationIssuer(tokenRepository, hasher, emailSender,
                new EmailVerificationProperties("http://localhost:3000/verify-email", Duration.ofHours(24)));
        User user = User.builder().id(UUID.randomUUID()).name("Rafael").email("rafael@example.com").build();

        String rawToken = issuer.issueToken(user);
        issuer.sendVerificationEmail(user, rawToken);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("rafael@example.com"), anyString(), body.capture());
        assertThat(body.getValue()).contains("http://localhost:3000/verify-email?token=" + rawToken);
    }

    @Test
    void sendVerificationEmail_whenBaseUrlAlreadyHasAQueryString_appendsTokenWithAmpersand() {
        issuer = new EmailVerificationIssuer(tokenRepository, hasher, emailSender,
                new EmailVerificationProperties("http://localhost:3000/verify-email?lang=pt", Duration.ofHours(24)));
        User user = User.builder().id(UUID.randomUUID()).name("Rafael").email("rafael@example.com").build();

        String rawToken = issuer.issueToken(user);
        issuer.sendVerificationEmail(user, rawToken);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("rafael@example.com"), anyString(), body.capture());
        assertThat(body.getValue()).contains("http://localhost:3000/verify-email?lang=pt&token=" + rawToken)
                .doesNotContain("?lang=pt?token=");
    }

    @Test
    void issueToken_invalidatesAnyPreviousTokenForTheUser() {
        issuer = new EmailVerificationIssuer(tokenRepository, hasher, emailSender,
                new EmailVerificationProperties("http://localhost:3000/verify-email", Duration.ofHours(24)));
        User user = User.builder().id(UUID.randomUUID()).name("Rafael").email("rafael@example.com").build();

        issuer.issueToken(user);

        verify(tokenRepository).invalidateAllByUserId(user.getId());
        verify(tokenRepository).save(any());
    }

}
