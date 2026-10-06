package com.gymplanner.plan.dto;

import com.gymplanner.equipment.EquipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** @param deleted wpis sprzętu został usunięty */
public record PlanEquipmentDto(UUID id, String name, EquipmentStatus status, boolean deleted,
        @Schema(nullable = true) String thumbnailUrl, @Schema(nullable = true) String photoUrl) {
}
