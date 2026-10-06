package com.gymplanner.exercise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Sprzęt, na którym można wykonać ćwiczenie. */
public record EquipmentOptionDto(UUID id, String name, @Schema(nullable = true) String thumbnailUrl) {
}
