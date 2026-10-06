package com.gymplanner.plan.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Pełna lista identyfikatorów w nowej kolejności. */
public record ReorderRequest(@NotNull List<UUID> ids) {
}
