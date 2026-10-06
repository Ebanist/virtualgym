package com.gymplanner.workout.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record UpdateSetRequest(
        @Min(0) @Max(1000) Integer reps,
        @DecimalMin("0") @DecimalMax("1000") BigDecimal weightKg,
        boolean completed) {
}
