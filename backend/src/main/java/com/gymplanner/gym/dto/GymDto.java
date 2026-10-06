package com.gymplanner.gym.dto;

import com.gymplanner.gym.GymStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record GymDto(
        UUID id,
        String name,
        String city,
        String address,
        @Schema(nullable = true) String description,
        GymStatus status,
        long memberCount,
        boolean member,
        Instant createdAt) {
}
