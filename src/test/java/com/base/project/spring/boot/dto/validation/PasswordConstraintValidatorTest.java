package com.base.project.spring.boot.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordConstraintValidatorTest {

    private final PasswordConstraintValidator validator = new PasswordConstraintValidator();

    @Test
    void isValid_nullIsValid_presenceIsNotBlanksJob() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void isValid_shorterThanEightCharsIsInvalid() {
        assertThat(validator.isValid("short1", null)).isFalse();
    }

    @Test
    void isValid_exactlyEightCharsIsValid() {
        assertThat(validator.isValid("12345678", null)).isTrue();
    }

    @Test
    void isValid_multibytePasswordWithin72BytesIsValid() {
        // 30 chars * 2 bytes (accented Latin-1 supplement) = 60 bytes, under the 72-byte limit.
        String password = "á".repeat(30);
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @Test
    void isValid_multibytePasswordOver72BytesIsInvalid() {
        // 20 chars * 4 bytes (emoji, astral plane) = 80 bytes, over BCrypt's 72-byte truncation limit.
        String password = "😀".repeat(20);
        assertThat(validator.isValid(password, null)).isFalse();
    }

    @Test
    void isValid_asciiPasswordAtExactly72BytesIsValid() {
        String password = "a".repeat(72);
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @Test
    void isValid_asciiPasswordOver72BytesIsInvalid() {
        String password = "a".repeat(73);
        assertThat(validator.isValid(password, null)).isFalse();
    }

}
