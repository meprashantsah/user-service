package com.prashant.user_service.service;

import com.prashant.user_service.dto.CreateUserProfileRequest;
import com.prashant.user_service.dto.UpdateUserProfileRequest;
import com.prashant.user_service.dto.UserProfileDto;
import com.prashant.user_service.entity.UserProfile;
import com.prashant.user_service.exception.UserNotFoundException;
import com.prashant.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

/**
 * User Profile Service - owns the user directory.
 *
 * Profiles are created by the auth-service during registration via the
 * internal endpoint. Everything else (viewing, searching, updating) is a
 * user-facing concern that flows through the API Gateway.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.avatar-url-prefix}")
    private String avatarUrlPrefix;

    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024; // 5 MB

    @Transactional
    public UserProfileDto createProfile(CreateUserProfileRequest request) {
        if (userProfileRepository.existsByUsername(request.username())) {
            log.warn("Profile already exists for username: {}", request.username());
            return mapToDto(userProfileRepository.findByUsername(request.username()).orElseThrow());
        }
        if (userProfileRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        UserProfile profile = UserProfile.builder()
                .id(request.id())
                .username(request.username())
                .displayName(request.displayName())
                .email(request.email())
                .build();

        UserProfile saved = userProfileRepository.save(profile);
        log.info("User profile created: {} ({})", saved.getUsername(), saved.getId());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID id) {
        return mapToDto(findProfile(id));
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfileByUsername(String username) {
        UserProfile profile = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        return mapToDto(profile);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> searchProfiles(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userProfileRepository
                .findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(query.trim(), query.trim())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public UserProfileDto updateProfile(UUID id, UpdateUserProfileRequest request) {
        UserProfile profile = findProfile(id);

        if (request.displayName() != null && !request.displayName().isBlank()) {
            profile.setDisplayName(request.displayName());
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.status() != null) {
            profile.setStatus(request.status());
        }

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Profile updated: {} ({})", saved.getUsername(), saved.getId());
        return mapToDto(saved);
    }

    @Transactional
    public void updateStatus(UUID id, com.prashant.user_service.entity.UserStatus status) {
        UserProfile profile = findProfile(id);
        profile.setStatus(status);
        userProfileRepository.save(profile);
    }

    /**
     * Persists an uploaded avatar image on disk and points the profile's
     * avatarUrl at it. Keeps the previous file extension so caching stays sane
     * and re-uploads overwrite the same file.
     */
    @Transactional
    public UserProfileDto uploadAvatar(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("Avatar must be smaller than 5 MB");
        }
        String type = file.getContentType() == null ? "" : file.getContentType();
        if (type.startsWith("image/") == false) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        String ext = switch (type) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/jpeg", "image/jpg" -> ".jpg";
            default -> ".jpg";
        };

        try {
            Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = userId + ext;
            Path target = dir.resolve(filename);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = avatarUrlPrefix + "/" + filename;
            UserProfile profile = findProfile(userId);
            profile.setAvatarUrl(url);
            userProfileRepository.save(profile);

            log.info("Avatar updated for user {} -> {}", userId, filename);
            return mapToDto(profile);
        } catch (IOException e) {
            throw new RuntimeException("Could not store avatar", e);
        }
    }

    /**
     * Resolves a stored avatar file so a controller can stream it.
     */
    public Resource resolveAvatar(String filename) {
        Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
        Path file = dir.resolve(filename).normalize();
        if (!file.startsWith(dir) || !Files.exists(file)) {
            throw new UserNotFoundException("Avatar not found: " + filename);
        }
        return UrlResource.from(file.toUri());
    }

    private UserProfile findProfile(UUID id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    private UserProfileDto mapToDto(UserProfile profile) {
        return new UserProfileDto(
                profile.getId(),
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getEmail(),
                profile.getAvatarUrl(),
                profile.getBio(),
                profile.getStatus(),
                profile.getCreatedAt()
        );
    }
}