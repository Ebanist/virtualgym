package com.gymplanner.workout.dto;

import com.gymplanner.workout.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionDto(
        UUID id,
        String title,
        UUID gymId,
        String gymName,
        @Schema(nullable = true) UUID planId,
        SessionStatus status,
        Instant startedAt,
        @Schema(nullable = true) Instant finishedAt,
        @Schema(nullable = true) String note,
        List<SessionExerciseDto> exercises) {
}
