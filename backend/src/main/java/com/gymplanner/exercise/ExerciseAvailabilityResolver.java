package com.gymplanner.exercise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Czysta logika: które ćwiczenia da się wykonać w siłowni i na którym sprzęcie.
 *
 * <p>Ćwiczenie jest dostępne, gdy:
 * <ul>
 *   <li>jest z masą ciała (zawsze), albo</li>
 *   <li>w siłowni jest dostępny sprzęt (nieusunięty, status ACTIVE) o typie wymaganym przez ćwiczenie, albo</li>
 *   <li>członek jawnie powiązał ćwiczenie z dostępnym sprzętem tej siłowni.</li>
 * </ul>
 * Sprzęt oznaczony jako usunięty z siłowni nie czyni ćwiczenia dostępnym.
 */
public final class ExerciseAvailabilityResolver {

    private ExerciseAvailabilityResolver() {
    }

    /** Wymagania ćwiczenia: masa ciała i/lub typy sprzętu (wystarczy jeden z nich). */
    public record ExerciseRequirement(UUID exerciseId, boolean bodyweight, Set<UUID> equipmentTypeIds) {
    }

    /** Sprzęt w siłowni; {@code available} = nieusunięty i ze statusem ACTIVE. */
    public record GymEquipment(UUID equipmentId, UUID equipmentTypeId, boolean available) {
    }

    /** Dostępne ćwiczenie i sprzęt, na którym można je wykonać (kolejność jak w danych wejściowych). */
    public record Availability(UUID exerciseId, boolean bodyweight, List<UUID> equipmentIds) {

        /** Czy pozycję planu (ćwiczenie + opcjonalny sprzęt) da się wykonać. */
        public boolean allows(UUID equipmentId) {
            return equipmentId == null ? bodyweight : equipmentIds.contains(equipmentId);
        }
    }

    /**
     * @param links jawne powiązania: id ćwiczenia → id sprzętu
     * @return dostępne ćwiczenia (klucz: id ćwiczenia), w kolejności wejściowej listy ćwiczeń
     */
    public static Map<UUID, Availability> resolve(Collection<ExerciseRequirement> exercises,
            Collection<GymEquipment> equipment, Map<UUID, Set<UUID>> links) {
        Map<UUID, Availability> result = new LinkedHashMap<>();
        for (ExerciseRequirement exercise : exercises) {
            Set<UUID> linked = links.getOrDefault(exercise.exerciseId(), Set.of());
            List<UUID> matching = new ArrayList<>();
            for (GymEquipment item : equipment) {
                if (!item.available()) {
                    continue;
                }
                boolean typeMatches = item.equipmentTypeId() != null
                        && exercise.equipmentTypeIds().contains(item.equipmentTypeId());
                if (typeMatches || linked.contains(item.equipmentId())) {
                    matching.add(item.equipmentId());
                }
            }
            if (exercise.bodyweight() || !matching.isEmpty()) {
                result.put(exercise.exerciseId(),
                        new Availability(exercise.exerciseId(), exercise.bodyweight(), List.copyOf(matching)));
            }
        }
        return result;
    }
}
