package com.gymplanner.plan.dto;

import com.gymplanner.exercise.MuscleGroup;
import java.util.UUID;

public record PlanExerciseDto(UUID id, String name, MuscleGroup primaryMuscle, boolean bodyweight) {
}
