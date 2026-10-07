package com.gymplanner.exercise;

import com.gymplanner.equipment.EquipmentType;
import com.gymplanner.equipment.dto.EquipmentTypeDto;
import com.gymplanner.exercise.dto.ExerciseDto;
import com.gymplanner.user.dto.UserRefDto;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class ExerciseMapper {

    private ExerciseMapper() {
    }

    /** @param equipmentIds sprzęt jawnie powiązany (podawany przy ćwiczeniach własnych – do edycji), może być null */
    public static ExerciseDto toDto(Exercise e, UUID currentUserId, List<UUID> equipmentIds) {
        boolean custom = e.getScope() == ExerciseScope.CUSTOM;
        return new ExerciseDto(e.getId(), e.getName(), e.getPrimaryMuscle(),
                e.getSecondaryMuscles().stream().sorted().toList(), e.getDescription(), e.isBodyweight(),
                e.getScope(), custom ? e.getVisibility() : null, custom && e.isAuthor(currentUserId),
                e.getGym() == null ? null : e.getGym().getId(),
                e.getEquipmentTypes().stream().sorted(Comparator.comparing(EquipmentType::getName))
                        .map(EquipmentTypeDto::from).toList(),
                equipmentIds,
                UserRefDto.from(e.getCreatedBy()));
    }

    public static ExerciseDto toDto(Exercise e, UUID currentUserId) {
        return toDto(e, currentUserId, null);
    }
}
