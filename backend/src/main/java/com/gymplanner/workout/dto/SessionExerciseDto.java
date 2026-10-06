package com.gymplanner.workout.dto;

import com.gymplanner.plan.dto.PlanEquipmentDto;
import com.gymplanner.plan.dto.PlanExerciseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** @param previous poprzedni wynik tego ćwiczenia (ostatni zakończony trening) */
public record SessionExerciseDto(
        UUID id,
        int position,
        PlanExerciseDto exercise,
        @Schema(nullable = true) PlanEquipmentDto equipment,
        @Schema(nullable = true) Integer targetRepsMin,
        @Schema(nullable = true) Integer targetRepsMax,
        @Schema(nullable = true) BigDecimal targetWeightKg,
        int restSeconds,
        @Schema(nullable = true) String note,
        List<SessionSetDto> sets,
        @Schema(nullable = true) ExerciseHistoryEntryDto previous) {
}
