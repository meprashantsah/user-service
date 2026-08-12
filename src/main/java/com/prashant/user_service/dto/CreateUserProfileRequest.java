package com.prashant.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserProfileRequest(
        UUID id,
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {
}