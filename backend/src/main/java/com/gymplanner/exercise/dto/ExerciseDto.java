package com.gymplanner.exercise.dto;

import com.gymplanner.equipment.dto.EquipmentTypeDto;
import com.gymplanner.exercise.ExerciseScope;
import com.gymplanner.exercise.ExerciseVisibility;
import com.gymplanner.exercise.MuscleGroup;
import com.gymplanner.user.dto.UserRefDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * @param visibility tylko dla ćwiczeń własnych (CUSTOM)
 * @param mine       ćwiczenie dodane przez bieżącego użytkownika (może je edytować i usuwać)
 */
public record ExerciseDto(
        UUID id,
        String name,
        MuscleGroup primaryMuscle,
        List<MuscleGroup> secondaryMuscles,
        @Schema(nullable = true) String description,
        boolean bodyweight,
        ExerciseScope scope,
        @Schema(nullable = true) ExerciseVisibility visibility,
        boolean mine,
        @Schema(nullable = true) UUID gymId,
        List<EquipmentTypeDto> equipmentTypes,
        @Schema(nullable = true) List<UUID> equipmentIds,
        @Schema(nullable = true) UserRefDto createdBy) {
}
