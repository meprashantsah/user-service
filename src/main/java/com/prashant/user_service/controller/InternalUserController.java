package com.prashant.user_service.controller;

import com.prashant.user_service.dto.CreateUserProfileRequest;
import com.prashant.user_service.dto.UserProfileDto;
import com.prashant.user_service.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal controller - called by other services (currently auth-service
 * during registration), NOT routed through the API Gateway. Access is guarded
 * by {@link com.prashant.user_service.config.InternalApiKeyFilter}.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserProfileService userProfileService;

    @PostMapping("/users")
    public ResponseEntity<UserProfileDto> createProfile(@Valid @RequestBody CreateUserProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userProfileService.createProfile(request));
    }
}