package com.base.project.spring.boot.security.totp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.samstevens.totp.code.DefaultCodeGenerator;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generateSecret_producesANonBlankSecret() {
        assertThat(totpService.generateSecret()).isNotBlank();
    }

    @Test
    void verifyCode_withCorrectCode_returnsTrue() throws Exception {
        String secret = totpService.generateSecret();
        String code = new DefaultCodeGenerator().generate(secret, System.currentTimeMillis() / 1000 / 30);

        assertThat(totpService.verifyCode(secret, code)).isTrue();
    }

    @Test
    void verifyCode_withWrongCode_returnsFalse() {
        String secret = totpService.generateSecret();

        assertThat(totpService.verifyCode(secret, "000000")).isFalse();
    }

    @Test
    void verifyCode_withNullSecret_returnsFalseInsteadOfThrowing() {
        assertThat(totpService.verifyCode(null, "123456")).isFalse();
    }

    @Test
    void verifyCode_withNullCode_returnsFalseInsteadOfThrowing() {
        String secret = totpService.generateSecret();

        assertThat(totpService.verifyCode(secret, null)).isFalse();
    }

    @Test
    void generateQrCodeDataUri_producesADataUri() {
        String secret = totpService.generateSecret();

        assertThat(totpService.generateQrCodeDataUri(secret, "rafael@example.com"))
                .startsWith("data:image/png;base64,");
    }

}
