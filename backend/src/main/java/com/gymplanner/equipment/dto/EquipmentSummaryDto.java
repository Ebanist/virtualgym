package com.gymplanner.equipment.dto;

import com.gymplanner.equipment.EquipmentCategory;
import com.gymplanner.equipment.EquipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record EquipmentSummaryDto(
        UUID id,
        String name,
        EquipmentCategory category,
        @Schema(nullable = true) EquipmentTypeDto equipmentType,
        @Schema(nullable = true) Integer quantity,
        EquipmentStatus status,
        @Schema(nullable = true) String thumbnailUrl,
        long openReportCount) {
}
