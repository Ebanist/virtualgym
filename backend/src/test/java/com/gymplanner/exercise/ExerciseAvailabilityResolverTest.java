package com.gymplanner.exercise;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymplanner.exercise.ExerciseAvailabilityResolver.Availability;
import com.gymplanner.exercise.ExerciseAvailabilityResolver.ExerciseRequirement;
import com.gymplanner.exercise.ExerciseAvailabilityResolver.GymEquipment;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExerciseAvailabilityResolverTest {

    static final UUID LAT_PULLDOWN = UUID.randomUUID();
    static final UUID BARBELL = UUID.randomUUID();
    static final UUID DUMBBELLS = UUID.randomUUID();

    final UUID pulldown = UUID.randomUUID();
    final UUID curl = UUID.randomUUID();
    final UUID pushUp = UUID.randomUUID();
    final UUID squat = UUID.randomUUID();

    final ExerciseRequirement pulldownReq = new ExerciseRequirement(pulldown, false, Set.of(LAT_PULLDOWN));
    final ExerciseRequirement curlReq = new ExerciseRequirement(curl, false, Set.of(BARBELL, DUMBBELLS));
    final ExerciseRequirement pushUpReq = new ExerciseRequirement(pushUp, true, Set.of());
    final ExerciseRequirement squatReq = new ExerciseRequirement(squat, false, Set.of(BARBELL));

    @Test
    void exerciseAvailableWhenGymHasEquipmentOfRequiredType() {
        UUID cable = UUID.randomUUID();
        Map<UUID, Availability> result = ExerciseAvailabilityResolver.resolve(List.of(pulldownReq, squatReq),
                List.of(new GymEquipment(cable, LAT_PULLDOWN, true)), Map.of());

        assertThat(result).containsOnlyKeys(pulldown);
        assertThat(result.get(pulldown).equipmentIds()).containsExactly(cable);
    }

    @Test
    void anyOfRequiredTypesIsEnoughAndAllMatchingEquipmentIsReturned() {
        UUID dumbbells = UUID.randomUUID();
        UUID barbell1 = UUID.randomUUID();
        UUID barbell2 = UUID.randomUUID();
        Map<UUID, Availability> result = ExerciseAvailabilityResolver.resolve(List.of(curlReq),
                List.of(new GymEquipment(barbell1, BARBELL, true), new GymEquipment(dumbbells, DUMBBELLS, true),
                        new GymEquipment(barbell2, BARBELL, true)),
                Map.of());

        assertThat(result.get(curl).equipmentIds()).containsExactly(barbell1, dumbbells, barbell2);
    }

    @Test
    void bodyweightExercisesAreAlwaysAvailable() {
        Map<UUID, Availability> result = ExerciseAvailabilityResolver.resolve(List.of(pushUpReq, pulldownReq),
                List.of(), Map.of());

        assertThat(result).containsOnlyKeys(pushUp);
        assertThat(result.get(pushUp).equipmentIds()).isEmpty();
        assertThat(result.get(pushUp).allows(null)).isTrue();
    }

    @Test
    void removedEquipmentDoesNotMakeExerciseAvailable() {
        UUID removed = UUID.randomUUID();
        Map<UUID, Availability> result = ExerciseAvailabilityResolver.resolve(List.of(pulldownReq),
                List.of(new GymEquipment(removed, LAT_PULLDOWN, false)), Map.of(pulldown, Set.of(removed)));

        assertThat(result).isEmpty();
    }

    @Test
    void explicitLinkMakesExerciseAvailableOnEquipmentWithoutType() {
        UUID untyped = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Map<UUID, Availability> result = ExerciseAvailabilityResolver.resolve(List.of(squatReq),
                List.of(new GymEquipment(untyped, null, true), new GymEquipment(other, null, true)),
                Map.of(squat, Set.of(untyped)));

        assertThat(result.get(squat).equipmentIds()).containsExactly(untyped);
    }

    @Test
    void allowsChecksEquipmentChoice() {
        UUID cable = UUID.randomUUID();
        Availability availability = ExerciseAvailabilityResolver.resolve(List.of(pulldownReq),
                List.of(new GymEquipment(cable, LAT_PULLDOWN, true)), Map.of()).get(pulldown);

        assertThat(availability.allows(cable)).isTrue();
        assertThat(availability.allows(UUID.randomUUID())).isFalse();
        assertThat(availability.allows(null)).isFalse(); // ćwiczenie wymaga sprzętu
    }

    @Test
    void bodyweightExerciseCanAlsoUseLinkedEquipment() {
        UUID box = UUID.randomUUID();
        Availability availability = ExerciseAvailabilityResolver.resolve(List.of(pushUpReq),
                List.of(new GymEquipment(box, null, true)), Map.of(pushUp, Set.of(box))).get(pushUp);

        assertThat(availability.allows(null)).isTrue();
        assertThat(availability.allows(box)).isTrue();
    }
}
