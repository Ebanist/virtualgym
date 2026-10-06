package com.gymplanner.equipment.dto;

import com.gymplanner.equipment.EquipmentCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateEquipmentRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotNull EquipmentCategory category,
        UUID equipmentTypeId,
        @Size(max = 2000) String description,
        @Min(1) @Max(999) Integer quantity) {
}
