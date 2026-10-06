package com.gymplanner.plan.dto;

import com.gymplanner.plan.PlanVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanDto(
        UUID id,
        String name,
        @Schema(nullable = true) String description,
        UUID gymId,
        String gymName,
        boolean archived,
        PlanVisibility visibility,
        List<PlanDayDto> days,
        int warningCount,
        Instant createdAt,
        Instant updatedAt) {
}
