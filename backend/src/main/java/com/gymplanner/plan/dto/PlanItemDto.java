package com.gymplanner.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/** @param equipmentUnavailable sprzęt usunięty lub oznaczony jako usunięty z siłowni – ostrzeżenie w planie */
public record PlanItemDto(
        UUID id,
        int position,
        PlanExerciseDto exercise,
        @Schema(nullable = true) PlanEquipmentDto equipment,
        int sets,
        int repsMin,
        int repsMax,
        @Schema(nullable = true) BigDecimal targetWeightKg,
        int restSeconds,
        @Schema(nullable = true) String note,
        boolean equipmentUnavailable) {
}
