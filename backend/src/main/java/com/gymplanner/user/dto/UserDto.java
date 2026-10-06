package com.gymplanner.user.dto;

import com.gymplanner.user.Role;
import com.gymplanner.user.User;
import java.time.Instant;
import java.util.UUID;

public record UserDto(UUID id, String email, String displayName, Role role, Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }
}
