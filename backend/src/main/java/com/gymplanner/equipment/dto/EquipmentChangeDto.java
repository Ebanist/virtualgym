package com.gymplanner.equipment.dto;

import com.gymplanner.equipment.ChangeType;
import com.gymplanner.equipment.FieldChange;
import com.gymplanner.user.dto.UserRefDto;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** {@code changes}: nazwa pola → wartość przed/po (name, category, equipmentType, description, quantity, status). */
public record EquipmentChangeDto(UUID id, UserRefDto user, Instant changedAt, ChangeType changeType,
        Map<String, FieldChange> changes) {
}
