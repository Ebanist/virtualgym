package com.gymplanner.plan;

import com.gymplanner.equipment.Equipment;
import com.gymplanner.exercise.Exercise;
import com.gymplanner.plan.dto.PlanDayDto;
import com.gymplanner.plan.dto.PlanDto;
import com.gymplanner.plan.dto.PlanEquipmentDto;
import com.gymplanner.plan.dto.PlanExerciseDto;
import com.gymplanner.plan.dto.PlanItemDto;
import com.gymplanner.plan.dto.PlanSummaryDto;
import com.gymplanner.storage.FileController;
import java.util.UUID;

public final class PlanMapper {

    private PlanMapper() {
    }

    static PlanDto toDto(WorkoutPlan plan) {
        return new PlanDto(plan.getId(), plan.getName(), plan.getDescription(), plan.getGym().getId(),
                plan.getGym().getName(), plan.isArchived(), plan.getVisibility(),
                plan.getDays().stream().map(PlanMapper::toDto).toList(), warningCount(plan), plan.getCreatedAt(),
                plan.getUpdatedAt());
    }

    static PlanSummaryDto toSummary(WorkoutPlan plan) {
        int exercises = plan.getDays().stream().mapToInt(d -> d.getItems().size()).sum();
        return new PlanSummaryDto(plan.getId(), plan.getName(), plan.getGym().getId(), plan.getGym().getName(),
                plan.isArchived(), plan.getDays().size(), exercises, warningCount(plan), plan.getUpdatedAt());
    }

    static PlanDayDto toDto(PlanDay day) {
        return new PlanDayDto(day.getId(), day.getName(), day.getPosition(),
                day.getItems().stream().map(PlanMapper::toDto).toList());
    }

    static PlanItemDto toDto(PlanItem item) {
        return new PlanItemDto(item.getId(), item.getPosition(), toDto(item.getExercise()),
                toDto(item.getEquipment()), item.getSets(), item.getRepsMin(), item.getRepsMax(),
                item.getTargetWeightKg(), item.getRestSeconds(), item.getNote(), item.isEquipmentUnavailable());
    }

    public static PlanExerciseDto toDto(Exercise e) {
        return new PlanExerciseDto(e.getId(), e.getName(), e.getPrimaryMuscle(), e.isBodyweight());
    }

    public static PlanEquipmentDto toDto(Equipment e) {
        if (e == null) {
            return null;
        }
        UUID photoId = e.getPhoto() == null ? null : e.getPhoto().getId();
        return new PlanEquipmentDto(e.getId(), e.getName(), e.getStatus(), e.isDeleted(),
                FileController.thumbnailUrl(photoId), FileController.url(photoId));
    }

    private static int warningCount(WorkoutPlan plan) {
        return (int) plan.getDays().stream()
                .flatMap(d -> d.getItems().stream())
                .filter(PlanItem::isEquipmentUnavailable)
                .count();
    }
}
