package com.gymplanner.plan.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** {@code equipmentId} null = ćwiczenie z masą ciała. */
public record PlanItemRequest(
        @NotNull UUID exerciseId,
        UUID equipmentId,
        @Min(1) @Max(20) int sets,
        @Min(1) @Max(100) int repsMin,
        @Min(1) @Max(100) int repsMax,
        @DecimalMin("0") @DecimalMax("1000") BigDecimal targetWeightKg,
        @Min(0) @Max(900) int restSeconds,
        @Size(max = 500) String note) {
}
