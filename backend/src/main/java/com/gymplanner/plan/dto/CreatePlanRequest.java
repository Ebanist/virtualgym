package com.gymplanner.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreatePlanRequest(
        @NotNull UUID gymId,
        @NotBlank @Size(min = 2, max = 100) String name,
        @Size(max = 2000) String description) {
}
