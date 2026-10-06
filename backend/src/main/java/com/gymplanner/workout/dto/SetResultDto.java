package com.gymplanner.workout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record SetResultDto(int setNumber, @Schema(nullable = true) Integer reps,
        @Schema(nullable = true) BigDecimal weightKg) {
}
