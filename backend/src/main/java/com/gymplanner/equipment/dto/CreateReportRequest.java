package com.gymplanner.equipment.dto;

import com.gymplanner.equipment.ReportType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** {@code duplicateOfId} – opcjonalnie wskazanie oryginału przy zgłoszeniu typu DUPLICATE. */
public record CreateReportRequest(@NotNull ReportType type, @Size(max = 1000) String comment, UUID duplicateOfId) {
}
