package com.gymplanner.plan.dto;

import java.time.Instant;
import java.util.UUID;

public record PlanSummaryDto(UUID id, String name, UUID gymId, String gymName, boolean archived, int dayCount,
        int exerciseCount, int warningCount, Instant updatedAt) {
}
