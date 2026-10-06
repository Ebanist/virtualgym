package com.gymplanner.exercise.dto;

/**
 * Ćwiczenie wykonywane na danym sprzęcie.
 *
 * @param linked  jawne powiązanie dodane przez członka (można je usunąć)
 * @param byType  dopasowanie przez typ sprzętu
 */
public record EquipmentExerciseDto(ExerciseDto exercise, boolean linked, boolean byType) {
}
