package com.prashant.user_service.service;

import com.prashant.user_service.dto.CreateUserProfileRequest;
import com.prashant.user_service.dto.UpdateUserProfileRequest;
import com.prashant.user_service.dto.UserProfileDto;
import com.prashant.user_service.entity.UserProfile;
import com.prashant.user_service.exception.UserNotFoundException;
import com.prashant.user_service.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository repository;

    private UserProfileService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(repository);
        userId = UUID.randomUUID();
    }

    @Test
    void createProfile_persistsNewProfile() {
        when(repository.existsByUsername("alice")).thenReturn(false);
        when(repository.existsByEmail("alice@example.com")).thenReturn(false);
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileDto dto = service.createProfile(
                new CreateUserProfileRequest(userId, "alice", "Alice", "alice@example.com"));

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(captor.capture());
        UserProfile saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(userId);
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getDisplayName()).isEqualTo("Alice");
        assertThat(dto.id()).isEqualTo(userId);
    }

    @Test
    void createProfile_isIdempotentWhenUsernameExists() {
        UserProfile existing = buildProfile();
        when(repository.existsByUsername("alice")).thenReturn(true);
        when(repository.findByUsername("alice")).thenReturn(Optional.of(existing));

        UserProfileDto dto = service.createProfile(
                new CreateUserProfileRequest(userId, "alice", "Alice", "alice@example.com"));

        assertThat(dto.username()).isEqualTo("alice");
        verify(repository, never()).save(any());
    }

    @Test
    void getProfile_throwsWhenNotFound() {
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void search_returnsMatches() {
        UserProfile existing = buildProfile();
        when(repository.findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase("ali", "ali"))
                .thenReturn(List.of(existing));

        List<UserProfileDto> results = service.searchProfiles("ali");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).username()).isEqualTo("alice");
    }

    @Test
    void updateProfile_updatesOnlyProvidedFields() {
        UserProfile existing = buildProfile();
        when(repository.findById(userId)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProfile(userId, new UpdateUserProfileRequest("Updated Alice", null, "Hello", null));

        verify(repository).save(existing);
        assertThat(existing.getDisplayName()).isEqualTo("Updated Alice");
        assertThat(existing.getBio()).isEqualTo("Hello");
        assertThat(existing.getAvatarUrl()).isNull();
    }

    private UserProfile buildProfile() {
        return UserProfile.builder()
                .id(userId)
                .username("alice")
                .displayName("Alice")
                .email("alice@example.com")
                .build();
    }
}