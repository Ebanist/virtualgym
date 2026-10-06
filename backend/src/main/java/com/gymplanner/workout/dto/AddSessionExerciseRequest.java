package com.gymplanner.workout.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddSessionExerciseRequest(
        @NotNull UUID exerciseId,
        UUID equipmentId,
        @Min(1) @Max(20) Integer sets,
        @Min(0) @Max(900) Integer restSeconds) {
}
