package com.gymplanner.exercise.dto;

import com.gymplanner.exercise.ExerciseVisibility;
import com.gymplanner.exercise.MuscleGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Własne ćwiczenie w siłowni: z masą ciała albo przypisane do co najmniej jednego sprzętu tej siłowni.
 * Te same pola służą do edycji. {@code visibility} domyślnie PRIVATE.
 */
public record CreateExerciseRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotNull MuscleGroup primaryMuscle,
        @Size(max = 10) List<MuscleGroup> secondaryMuscles,
        @Size(max = 2000) String description,
        boolean bodyweight,
        @Size(max = 20) List<UUID> equipmentIds,
        ExerciseVisibility visibility) {
}
