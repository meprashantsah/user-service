package com.prashant.user_service.controller;

import com.prashant.user_service.dto.UpdateUserProfileRequest;
import com.prashant.user_service.dto.UserProfileDto;
import com.prashant.user_service.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * User Profile Controller - public (gateway-protected) user directory API.
 *
 * The gateway validates the JWT and forwards X-User-Id / X-Username headers,
 * which identify the calling user.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(userProfileService.getProfile(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserProfileDto>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(userProfileService.searchProfiles(query));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileDto> updateProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(userId, request));
    }
}