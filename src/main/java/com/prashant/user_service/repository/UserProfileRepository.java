package com.prashant.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.prashant.user_service.entity.UserProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUsername(String username);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<UserProfile> findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String displayName, String username);
}