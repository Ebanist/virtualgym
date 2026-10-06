package com.gymplanner.exercise;

import com.gymplanner.equipment.Equipment;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Migawka ćwiczeń dostępnych w siłowni: wynik {@link ExerciseAvailabilityResolver} wraz z encjami.
 * Używana przez listę dostępnych ćwiczeń i walidację pozycji planu.
 */
public record GymExerciseCatalog(
        List<Exercise> exercises,
        Map<UUID, Equipment> equipmentById,
        Map<UUID, ExerciseAvailabilityResolver.Availability> availability) {

    public boolean isAvailable(UUID exerciseId, UUID equipmentId) {
        ExerciseAvailabilityResolver.Availability a = availability.get(exerciseId);
        return a != null && a.allows(equipmentId);
    }
}
