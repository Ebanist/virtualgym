package com.gymplanner.equipment.dto;

import com.gymplanner.equipment.ReportStatus;
import com.gymplanner.equipment.ReportType;
import com.gymplanner.user.dto.UserRefDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record EquipmentReportDto(
        UUID id,
        UUID equipmentId,
        ReportType type,
        @Schema(nullable = true) String comment,
        @Schema(nullable = true) EquipmentRefDto duplicateOf,
        ReportStatus status,
        UserRefDto reporter,
        Instant createdAt,
        @Schema(nullable = true) UserRefDto resolvedBy,
        @Schema(nullable = true) Instant resolvedAt) {
}
