package com.base.project.spring.boot.dto;

import com.base.project.spring.boot.dto.validation.ValidPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "name is required") @Size(max = 255, message = "name must be at most 255 characters") String name,

        @NotBlank(message = "email is required") @Email(message = "email must be a valid email address") @Size(max = 255, message = "email must be at most 255 characters") String email,

        @NotBlank(message = "password is required") @ValidPassword String password) {
}
