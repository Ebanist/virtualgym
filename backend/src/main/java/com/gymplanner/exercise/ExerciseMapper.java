package com.gymplanner.exercise;

import com.gymplanner.equipment.EquipmentType;
import com.gymplanner.equipment.dto.EquipmentTypeDto;
import com.gymplanner.exercise.dto.ExerciseDto;
import com.gymplanner.user.dto.UserRefDto;
import java.util.Comparator;

public final class ExerciseMapper {

    private ExerciseMapper() {
    }

    public static ExerciseDto toDto(Exercise e) {
        return new ExerciseDto(e.getId(), e.getName(), e.getPrimaryMuscle(),
                e.getSecondaryMuscles().stream().sorted().toList(), e.getDescription(), e.isBodyweight(),
                e.getScope(), e.getGym() == null ? null : e.getGym().getId(),
                e.getEquipmentTypes().stream().sorted(Comparator.comparing(EquipmentType::getName))
                        .map(EquipmentTypeDto::from).toList(),
                UserRefDto.from(e.getCreatedBy()));
    }
}
