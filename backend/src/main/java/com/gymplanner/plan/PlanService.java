package com.gymplanner.plan;

import com.gymplanner.common.error.BusinessRuleException;
import com.gymplanner.common.error.NotFoundException;
import com.gymplanner.equipment.Equipment;
import com.gymplanner.exercise.Exercise;
import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.GymExerciseCatalog;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymAccessService;
import com.gymplanner.plan.dto.CreatePlanRequest;
import com.gymplanner.plan.dto.PlanDayRequest;
import com.gymplanner.plan.dto.PlanDto;
import com.gymplanner.plan.dto.PlanItemRequest;
import com.gymplanner.plan.dto.PlanSummaryDto;
import com.gymplanner.plan.dto.UpdatePlanRequest;
import com.gymplanner.user.UserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Plany treningowe. Plan widzi i edytuje wyłącznie właściciel (cudzy plan => 404). */
@Service
public class PlanService {

    private final WorkoutPlanRepository plans;
    private final GymAccessService access;
    private final ExerciseService exerciseService;
    private final UserRepository users;
    private final Clock clock;

    public PlanService(WorkoutPlanRepository plans, GymAccessService access, ExerciseService exerciseService,
            UserRepository users, Clock clock) {
        this.plans = plans;
        this.access = access;
        this.exerciseService = exerciseService;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PlanSummaryDto> list(UUID userId, boolean archived, UUID gymId) {
        return plans.findOwnedList(userId, archived, gymId).stream().map(PlanMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public PlanDto get(UUID userId, UUID planId) {
        return PlanMapper.toDto(getOwned(userId, planId));
    }

    @Transactional
    public PlanDto create(UUID userId, CreatePlanRequest request) {
        Gym gym = access.getGym(request.gymId());
        access.requireMember(userId, gym.getId());
        WorkoutPlan plan = new WorkoutPlan(users.getReferenceById(userId), gym, request.name().trim(),
                blankToNull(request.description()));
        return PlanMapper.toDto(plans.saveAndFlush(plan));
    }

    @Transactional
    public PlanDto update(UUID userId, UUID planId, UpdatePlanRequest request) {
        WorkoutPlan plan = getOwned(userId, planId);
        plan.setName(request.name().trim());
        plan.setDescription(blankToNull(request.description()));
        return flushAndMap(plan);
    }

    @Transactional
    public void delete(UUID userId, UUID planId) {
        getOwned(userId, planId).markDeleted(clock.instant());
    }

    @Transactional
    public PlanDto setArchived(UUID userId, UUID planId, boolean archived) {
        WorkoutPlan plan = getOwned(userId, planId);
        plan.setArchived(archived);
        return flushAndMap(plan);
    }

    /** Kopia planu (dni i pozycje) – zawsze aktywna, w tej samej siłowni. Domyślna nazwa: „… (kopia)”. */
    @Transactional
    public PlanDto copy(UUID userId, UUID planId, String requestedName) {
        WorkoutPlan source = getOwned(userId, planId);
        String name = requestedName != null && !requestedName.isBlank() ? requestedName.trim()
                : truncate(source.getName() + " (kopia)", 100);
        WorkoutPlan copy = new WorkoutPlan(source.getOwner(), source.getGym(), name, source.getDescription());
        for (PlanDay day : source.getDays()) {
            PlanDay newDay = copy.addDay(day.getName());
            day.getItems().forEach(item -> newDay.addItem(item.values()));
        }
        return PlanMapper.toDto(plans.saveAndFlush(copy));
    }

    @Transactional
    public PlanDto addDay(UUID userId, UUID planId, PlanDayRequest request) {
        WorkoutPlan plan = getOwned(userId, planId);
        plan.addDay(request.name().trim());
        return flushAndMap(plan);
    }

    @Transactional
    public PlanDto renameDay(UUID userId, UUID planId, UUID dayId, PlanDayRequest request) {
        WorkoutPlan plan = getOwned(userId, planId);
        findDay(plan, dayId).setName(request.name().trim());
        return flushAndMap(plan);
    }

    @Transactional
    public PlanDto deleteDay(UUID userId, UUID planId, UUID dayId) {
        WorkoutPlan plan = getOwned(userId, planId);
        plan.removeDay(findDay(plan, dayId));
        return flushAndMap(plan);
    }

    @Transactional
    public PlanDto reorderDays(UUID userId, UUID planId, List<UUID> ids) {
        WorkoutPlan plan = getOwned(userId, planId);
        reorder(plan.getDays(), ids);
        return flushAndMap(plan);
    }

    @Transactional
    public PlanDto moveDay(UUID userId, UUID planId, UUID dayId, MoveDirection direction) {
        WorkoutPlan plan = getOwned(userId, planId);
        move(plan.getDays(), findDay(plan, dayId), direction);
        return flushAndMap(plan);
    }

    @Transactional
    public PlanDto addItem(UUID userId, UUID planId, UUID dayId, PlanItemRequest request) {
        WorkoutPlan plan = getOwned(userId, planId);
        PlanDay day = findDay(plan, dayId);
        day.addItem(validatedValues(plan, request));
        return flushAndMap(plan);
    }

    @Transactional
    public PlanDto updateItem(UUID userId, UUID planId, UUID itemId, PlanItemRequest request) {
        WorkoutPlan plan = getOwned(userId, planId);
        findItem(plan, itemId).apply(validatedValues(plan, request));
        return flushAndMap(plan);
    }

    @Transactional
    public PlanDto deleteItem(UUID userId, UUID planId, UUID itemId) {
        WorkoutPlan plan = getOwned(userId, planId);
        PlanItem item = findItem(plan, itemId);
        item.getDay().removeItem(item);
        return flushAndMap(plan);
    }

    @Transactional
    public PlanDto reorderItems(UUID userId, UUID planId, UUID dayId, List<UUID> ids) {
        WorkoutPlan plan = getOwned(userId, planId);
        reorder(findDay(plan, dayId).getItems(), ids);
        return flushAndMap(plan);
    }

    @Transactional
    public PlanDto moveItem(UUID userId, UUID planId, UUID itemId, MoveDirection direction) {
        WorkoutPlan plan = getOwned(userId, planId);
        PlanItem item = findItem(plan, itemId);
        move(item.getDay().getItems(), item, direction);
        return flushAndMap(plan);
    }

    /** Plan właściciela (nieusunięty) – również dla modułu treningów. */
    @Transactional(readOnly = true)
    public WorkoutPlan getOwned(UUID userId, UUID planId) {
        return plans.findOwned(planId, userId).orElseThrow(() -> new NotFoundException("Plan"));
    }

    /**
     * Walidacja pozycji: tylko ćwiczenia możliwe do wykonania na sprzęcie siłowni planu
     * (lub z masą ciała), a wybrany sprzęt musi do tego ćwiczenia pasować.
     */
    private PlanItem.Values validatedValues(WorkoutPlan plan, PlanItemRequest request) {
        if (request.repsMax() < request.repsMin()) {
            throw new BusinessRuleException("invalid_reps_range", "repsMax must be greater or equal to repsMin");
        }
        GymExerciseCatalog catalog = exerciseService.catalog(plan.getGym().getId(), plan.getOwner().getId());
        if (!catalog.isAvailable(request.exerciseId(), request.equipmentId())) {
            throw new BusinessRuleException("exercise_not_available",
                    "This exercise cannot be done on the selected equipment in this gym");
        }
        Exercise exercise = catalog.exercises().stream()
                .filter(e -> e.getId().equals(request.exerciseId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Exercise"));
        Equipment equipment = request.equipmentId() == null ? null
                : catalog.equipmentById().get(request.equipmentId());
        return new PlanItem.Values(exercise, equipment, request.sets(), request.repsMin(), request.repsMax(),
                request.targetWeightKg(), request.restSeconds(), blankToNull(request.note()));
    }

    private PlanDto flushAndMap(WorkoutPlan plan) {
        plans.saveAndFlush(plan);
        return PlanMapper.toDto(plan);
    }

    private static PlanDay findDay(WorkoutPlan plan, UUID dayId) {
        return plan.getDays().stream().filter(d -> d.getId().equals(dayId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Plan day"));
    }

    private static PlanItem findItem(WorkoutPlan plan, UUID itemId) {
        return plan.getDays().stream().flatMap(d -> d.getItems().stream())
                .filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Plan item"));
    }

    private static <T extends WorkoutPlan.Positioned> void reorder(List<T> list, List<UUID> ids) {
        Map<UUID, T> byId = list.stream().collect(Collectors.toMap(WorkoutPlan.Positioned::getId, Function.identity()));
        if (ids.size() != list.size() || !byId.keySet().equals(new HashSet<>(ids))) {
            throw new BusinessRuleException("invalid_order", "The order must contain every element exactly once");
        }
        List<T> ordered = new ArrayList<>(ids.stream().map(byId::get).toList());
        list.clear();
        list.addAll(ordered);
        WorkoutPlan.renumber(list);
    }

    private static <T extends WorkoutPlan.Positioned> void move(List<T> list, T element, MoveDirection direction) {
        int index = list.indexOf(element);
        int target = direction == MoveDirection.UP ? index - 1 : index + 1;
        if (target < 0 || target >= list.size()) {
            return;
        }
        Collections.swap(list, index, target);
        WorkoutPlan.renumber(list);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
