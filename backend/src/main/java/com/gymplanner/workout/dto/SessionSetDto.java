package com.gymplanner.workout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

public record SessionSetDto(UUID id, int setNumber, @Schema(nullable = true) Integer reps,
        @Schema(nullable = true) BigDecimal weightKg, boolean completed) {
}
