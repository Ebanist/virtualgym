package com.gymplanner.plan;

import com.gymplanner.auth.AuthUser;
import com.gymplanner.plan.dto.CopyPlanRequest;
import com.gymplanner.plan.dto.CreatePlanRequest;
import com.gymplanner.plan.dto.PlanDayRequest;
import com.gymplanner.plan.dto.PlanDto;
import com.gymplanner.plan.dto.PlanItemRequest;
import com.gymplanner.plan.dto.PlanSummaryDto;
import com.gymplanner.plan.dto.ReorderRequest;
import com.gymplanner.plan.dto.UpdatePlanRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Plany treningowe. Operacje modyfikujące zwracają cały plan (prosty stan po stronie klienta). */
@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "Moje plany (aktywne lub zarchiwizowane), opcjonalnie dla siłowni")
    public List<PlanSummaryDto> list(AuthUser user, @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(required = false) UUID gymId) {
        return planService.list(user.id(), archived, gymId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Nowy plan w siłowni, do której należę")
    public PlanDto create(AuthUser user, @Valid @RequestBody CreatePlanRequest request) {
        return planService.create(user.id(), request);
    }

    @GetMapping("/{planId}")
    public PlanDto get(AuthUser user, @PathVariable UUID planId) {
        return planService.get(user.id(), planId);
    }

    @PutMapping("/{planId}")
    public PlanDto update(AuthUser user, @PathVariable UUID planId, @Valid @RequestBody UpdatePlanRequest request) {
        return planService.update(user.id(), planId, request);
    }

    @DeleteMapping("/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Usunięcie planu (miękkie)")
    public void delete(AuthUser user, @PathVariable UUID planId) {
        planService.delete(user.id(), planId);
    }

    @PostMapping("/{planId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanDto copy(AuthUser user, @PathVariable UUID planId,
            @Valid @RequestBody(required = false) CopyPlanRequest request) {
        return planService.copy(user.id(), planId, request == null ? null : request.name());
    }

    @PostMapping("/{planId}/archive")
    public PlanDto archive(AuthUser user, @PathVariable UUID planId) {
        return planService.setArchived(user.id(), planId, true);
    }

    @PostMapping("/{planId}/unarchive")
    public PlanDto unarchive(AuthUser user, @PathVariable UUID planId) {
        return planService.setArchived(user.id(), planId, false);
    }

    @PostMapping("/{planId}/days")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanDto addDay(AuthUser user, @PathVariable UUID planId, @Valid @RequestBody PlanDayRequest request) {
        return planService.addDay(user.id(), planId, request);
    }

    @PutMapping("/{planId}/days/{dayId}")
    public PlanDto renameDay(AuthUser user, @PathVariable UUID planId, @PathVariable UUID dayId,
            @Valid @RequestBody PlanDayRequest request) {
        return planService.renameDay(user.id(), planId, dayId, request);
    }

    @DeleteMapping("/{planId}/days/{dayId}")
    public PlanDto deleteDay(AuthUser user, @PathVariable UUID planId, @PathVariable UUID dayId) {
        return planService.deleteDay(user.id(), planId, dayId);
    }

    @PutMapping("/{planId}/days/order")
    @Operation(summary = "Nowa kolejność dni (pełna lista id)")
    public PlanDto reorderDays(AuthUser user, @PathVariable UUID planId, @Valid @RequestBody ReorderRequest request) {
        return planService.reorderDays(user.id(), planId, request.ids());
    }

    @PostMapping("/{planId}/days/{dayId}/move")
    @Operation(summary = "Przesunięcie dnia o jedną pozycję (przyciski góra/dół)")
    public PlanDto moveDay(AuthUser user, @PathVariable UUID planId, @PathVariable UUID dayId,
            @RequestParam MoveDirection direction) {
        return planService.moveDay(user.id(), planId, dayId, direction);
    }

    @PostMapping("/{planId}/days/{dayId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dodanie ćwiczenia do dnia – tylko ćwiczenia dostępne na sprzęcie siłowni planu "
            + "(422 exercise_not_available)")
    public PlanDto addItem(AuthUser user, @PathVariable UUID planId, @PathVariable UUID dayId,
            @Valid @RequestBody PlanItemRequest request) {
        return planService.addItem(user.id(), planId, dayId, request);
    }

    @PutMapping("/{planId}/days/{dayId}/items/order")
    @Operation(summary = "Nowa kolejność ćwiczeń w dniu (pełna lista id)")
    public PlanDto reorderItems(AuthUser user, @PathVariable UUID planId, @PathVariable UUID dayId,
            @Valid @RequestBody ReorderRequest request) {
        return planService.reorderItems(user.id(), planId, dayId, request.ids());
    }

    @PutMapping("/{planId}/items/{itemId}")
    public PlanDto updateItem(AuthUser user, @PathVariable UUID planId, @PathVariable UUID itemId,
            @Valid @RequestBody PlanItemRequest request) {
        return planService.updateItem(user.id(), planId, itemId, request);
    }

    @DeleteMapping("/{planId}/items/{itemId}")
    public PlanDto deleteItem(AuthUser user, @PathVariable UUID planId, @PathVariable UUID itemId) {
        return planService.deleteItem(user.id(), planId, itemId);
    }

    @PostMapping("/{planId}/items/{itemId}/move")
    @Operation(summary = "Przesunięcie ćwiczenia o jedną pozycję w dniu (przyciski góra/dół)")
    public PlanDto moveItem(AuthUser user, @PathVariable UUID planId, @PathVariable UUID itemId,
            @RequestParam MoveDirection direction) {
        return planService.moveItem(user.id(), planId, itemId, direction);
    }
}
