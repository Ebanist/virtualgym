package com.gymplanner.equipment.dto;

import com.gymplanner.equipment.EquipmentCategory;
import com.gymplanner.equipment.EquipmentType;
import java.util.UUID;

public record EquipmentTypeDto(UUID id, String code, String name, EquipmentCategory category) {

    public static EquipmentTypeDto from(EquipmentType type) {
        return type == null ? null : new EquipmentTypeDto(type.getId(), type.getCode(), type.getName(),
                type.getCategory());
    }
}
