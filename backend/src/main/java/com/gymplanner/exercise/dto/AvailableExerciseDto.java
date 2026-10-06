package com.gymplanner.exercise.dto;

import java.util.List;

/** Ćwiczenie dostępne w siłowni + sprzęt, na którym można je wykonać (pusta lista = tylko masa ciała). */
public record AvailableExerciseDto(ExerciseDto exercise, List<EquipmentOptionDto> equipment) {
}
