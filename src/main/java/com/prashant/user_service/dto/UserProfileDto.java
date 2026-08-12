package com.prashant.user_service.dto;

import com.prashant.user_service.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String username,
        String displayName,
        String email,
        String avatarUrl,
        String bio,
        UserStatus status,
        LocalDateTime createdAt
) {
}