package com.gymplanner.plan.dto;

import java.util.List;
import java.util.UUID;

public record PlanDayDto(UUID id, String name, int position, List<PlanItemDto> items) {
}
