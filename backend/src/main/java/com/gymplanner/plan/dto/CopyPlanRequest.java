package com.gymplanner.plan.dto;

import jakarta.validation.constraints.Size;

/** {@code name} opcjonalne – domyślnie „&lt;nazwa&gt; (kopia)”. */
public record CopyPlanRequest(@Size(min = 2, max = 100) String name) {
}
