package com.base.project.spring.boot.dto.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates password length the way BCryptPasswordEncoder actually enforces it: BCrypt silently truncates its input at
 * 72 UTF-8 bytes, so a char-counting {@code @Size(max = 72)} lets multibyte passwords (emoji, accented characters)
 * through validation while BCrypt hashes fewer effective characters than the user typed.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConstraintValidator.class)
public @interface ValidPassword {

    String message() default "password must be at least 8 characters and at most 72 bytes (UTF-8) long";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
