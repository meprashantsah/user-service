package com.prashant.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Profile - The user directory entity owned by the user-service.
 *
 * The id is shared with the account identity created by the auth-service so
 * that both services refer to the same logical user.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.OFFLINE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}