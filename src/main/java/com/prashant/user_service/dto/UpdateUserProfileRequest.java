package com.prashant.user_service.dto;

import jakarta.validation.constraints.Size;
import com.prashant.user_service.entity.UserStatus;

public record UpdateUserProfileRequest(
        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName,
        @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        String avatarUrl,
        @Size(max = 500, message = "Bio must not exceed 500 characters")
        String bio,
        UserStatus status
) {
}