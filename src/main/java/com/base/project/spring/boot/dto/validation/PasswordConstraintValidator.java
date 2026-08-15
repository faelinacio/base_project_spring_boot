package com.base.project.spring.boot.dto.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH_CHARS = 8;
    private static final int MAX_LENGTH_BYTES = 72; // BCryptPasswordEncoder's hard truncation limit

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true; // presence is @NotBlank's job
        }
        if (password.length() < MIN_LENGTH_CHARS) {
            return false;
        }
        return password.getBytes(StandardCharsets.UTF_8).length <= MAX_LENGTH_BYTES;
    }

}
