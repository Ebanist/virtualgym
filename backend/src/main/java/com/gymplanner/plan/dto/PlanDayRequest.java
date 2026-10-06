package com.gymplanner.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlanDayRequest(@NotBlank @Size(max = 100) String name) {
}
