package com.gymplanner.equipment.dto;

import com.gymplanner.equipment.EquipmentCategory;
import com.gymplanner.equipment.EquipmentSource;
import com.gymplanner.equipment.EquipmentStatus;
import com.gymplanner.user.dto.UserRefDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * @param member  czy bieżący użytkownik należy do siłowni (może edytować)
 * @param version wersja do optymistycznej kontroli współbieżności przy edycji
 */
public record EquipmentDto(
        UUID id,
        UUID gymId,
        String gymName,
        String name,
        EquipmentCategory category,
        @Schema(nullable = true) EquipmentTypeDto equipmentType,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) Integer quantity,
        EquipmentStatus status,
        EquipmentSource source,
        boolean verified,
        @Schema(nullable = true) String photoUrl,
        @Schema(nullable = true) String thumbnailUrl,
        UserRefDto createdBy,
        Instant createdAt,
        Instant updatedAt,
        long version,
        long openReportCount,
        boolean member) {
}
