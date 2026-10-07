package com.gymplanner.workout;

import com.gymplanner.common.error.BusinessRuleException;
import com.gymplanner.common.error.ConflictException;
import com.gymplanner.common.error.NotFoundException;
import com.gymplanner.common.web.PageResponse;
import com.gymplanner.equipment.Equipment;
import com.gymplanner.exercise.Exercise;
import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.GymExerciseCatalog;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymAccessService;
import com.gymplanner.plan.PlanDay;
import com.gymplanner.plan.PlanMapper;
import com.gymplanner.plan.PlanService;
import com.gymplanner.plan.WorkoutPlan;
import com.gymplanner.user.UserRepository;
import com.gymplanner.workout.dto.AddSessionExerciseRequest;
import com.gymplanner.workout.dto.ExerciseHistoryEntryDto;
import com.gymplanner.workout.dto.SessionDto;
import com.gymplanner.workout.dto.SessionExerciseDto;
import com.gymplanner.workout.dto.SessionSetDto;
import com.gymplanner.workout.dto.SessionSummaryDto;
import com.gymplanner.workout.dto.SetResultDto;
import com.gymplanner.workout.dto.StartSessionRequest;
import com.gymplanner.workout.dto.UpdateSetRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tryb treningu i dziennik. Trening widzi i modyfikuje tylko jego właściciel (cudzy => 404). */
@Service
public class WorkoutService {

    private static final int DEFAULT_SETS = 3;
    private static final int DEFAULT_REST = 90;

    private final WorkoutSessionRepository sessions;
    private final SessionExerciseRepository sessionExercises;
    private final PlanService planService;
    private final ExerciseService exerciseService;
    private final GymAccessService access;
    private final UserRepository users;
    private final Clock clock;

    public WorkoutService(WorkoutSessionRepository sessions, SessionExerciseRepository sessionExercises,
            PlanService planService, ExerciseService exerciseService, GymAccessService access, UserRepository users,
            Clock clock) {
        this.sessions = sessions;
        this.sessionExercises = sessionExercises;
        this.planService = planService;
        this.exerciseService = exerciseService;
        this.access = access;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public SessionDto start(UUID userId, StartSessionRequest request) {
        sessions.findActive(userId).ifPresent(active -> {
            throw new ConflictException("session_already_active", "Finish or abandon the current workout first",
                    Map.of("activeSessionId", active.getId()));
        });
        WorkoutSession session;
        if (request.planId() != null && request.planDayId() != null) {
            WorkoutPlan plan = planService.getOwned(userId, request.planId());
            PlanDay day = plan.getDays().stream().filter(d -> d.getId().equals(request.planDayId())).findFirst()
                    .orElseThrow(() -> new NotFoundException("Plan day"));
            session = new WorkoutSession(users.getReferenceById(userId), plan.getGym(), plan, day,
                    truncate(plan.getName() + " – " + day.getName(), 220), clock.instant());
            day.getItems().forEach(item -> session.addExercise(new SessionExercise.Target(item.getExercise(),
                    item.getEquipment(), item.getSets(), item.getRepsMin(), item.getRepsMax(),
                    item.getTargetWeightKg(), item.getRestSeconds(), item.getNote())));
        } else if (request.gymId() != null && request.planId() == null) {
            Gym gym = access.getGym(request.gymId());
            access.requireMember(userId, gym.getId());
            session = new WorkoutSession(users.getReferenceById(userId), gym, null, null, gym.getName(),
                    clock.instant());
        } else {
            throw new BusinessRuleException("invalid_session_source", "Provide planId and planDayId, or gymId");
        }
        return toDto(sessions.saveAndFlush(session), userId);
    }

    @Transactional(readOnly = true)
    public Optional<SessionDto> active(UUID userId) {
        return sessions.findActive(userId).map(s -> toDto(s, userId));
    }

    @Transactional(readOnly = true)
    public SessionDto get(UUID userId, UUID sessionId) {
        return toDto(getOwned(userId, sessionId), userId);
    }

    @Transactional
    public SessionDto updateSet(UUID userId, UUID sessionId, UUID setId, UpdateSetRequest request) {
        WorkoutSession session = getActiveOwned(userId, sessionId);
        SessionSet set = session.getExercises().stream().flatMap(e -> e.getSets().stream())
                .filter(s -> s.getId().equals(setId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Set"));
        set.update(request.reps(), request.weightKg(), request.completed(), clock.instant());
        return flushAndMap(session, userId);
    }

    /** Dodatkowa seria – kopiuje ciężar i powtórzenia z ostatniej serii. */
    @Transactional
    public SessionDto addSet(UUID userId, UUID sessionId, UUID sessionExerciseId) {
        WorkoutSession session = getActiveOwned(userId, sessionId);
        SessionExercise exercise = findExercise(session, sessionExerciseId);
        List<SessionSet> sets = exercise.getSets();
        SessionSet last = sets.isEmpty() ? null : sets.get(sets.size() - 1);
        if (sets.size() >= 30) {
            throw new BusinessRuleException("too_many_sets", "Too many sets");
        }
        exercise.addSet(last == null ? exercise.getTargetWeightKg() : last.getWeightKg(),
                last == null ? null : last.getReps());
        return flushAndMap(session, userId);
    }

    @Transactional
    public SessionDto removeSet(UUID userId, UUID sessionId, UUID setId) {
        WorkoutSession session = getActiveOwned(userId, sessionId);
        SessionSet set = session.getExercises().stream().flatMap(e -> e.getSets().stream())
                .filter(s -> s.getId().equals(setId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Set"));
        set.getSessionExercise().removeSet(set);
        return flushAndMap(session, userId);
    }

    /** Dodanie ćwiczenia (trening ad hoc lub dodatkowe ćwiczenie) – tylko dostępne w siłowni treningu. */
    @Transactional
    public SessionDto addExercise(UUID userId, UUID sessionId, AddSessionExerciseRequest request) {
        WorkoutSession session = getActiveOwned(userId, sessionId);
        GymExerciseCatalog catalog = exerciseService.catalog(session.getGym().getId(), userId);
        if (!catalog.isAvailable(request.exerciseId(), request.equipmentId())) {
            throw new BusinessRuleException("exercise_not_available",
                    "This exercise cannot be done on the selected equipment in this gym");
        }
        Exercise exercise = catalog.exercises().stream().filter(e -> e.getId().equals(request.exerciseId()))
                .findFirst().orElseThrow(() -> new NotFoundException("Exercise"));
        Equipment equipment = request.equipmentId() == null ? null
                : catalog.equipmentById().get(request.equipmentId());
        int sets = request.sets() == null ? DEFAULT_SETS : request.sets();
        int rest = request.restSeconds() == null ? DEFAULT_REST : request.restSeconds();
        session.addExercise(new SessionExercise.Target(exercise, equipment, sets, null, null, null, rest, null));
        return flushAndMap(session, userId);
    }

    @Transactional
    public SessionDto removeExercise(UUID userId, UUID sessionId, UUID sessionExerciseId) {
        WorkoutSession session = getActiveOwned(userId, sessionId);
        session.removeExercise(findExercise(session, sessionExerciseId));
        return flushAndMap(session, userId);
    }

    @Transactional
    public SessionDto finish(UUID userId, UUID sessionId, String note) {
        WorkoutSession session = getActiveOwned(userId, sessionId);
        session.finish(clock.instant(), note == null || note.isBlank() ? null : note.trim());
        return flushAndMap(session, userId);
    }

    @Transactional
    public void abandon(UUID userId, UUID sessionId) {
        getActiveOwned(userId, sessionId).abandon(clock.instant());
    }

    @Transactional(readOnly = true)
    public PageResponse<SessionSummaryDto> history(UUID userId, int page, int size) {
        return PageResponse.of(sessions.findHistory(userId, PageRequest.of(page, size)), WorkoutService::toSummary);
    }

    @Transactional(readOnly = true)
    public List<ExerciseHistoryEntryDto> exerciseHistory(UUID userId, UUID exerciseId, int limit) {
        return sessionExercises.findFinishedByExercise(userId, exerciseId, PageRequest.of(0, limit)).stream()
                .map(WorkoutService::toHistoryEntry)
                .toList();
    }

    private WorkoutSession getOwned(UUID userId, UUID sessionId) {
        return sessions.findOwned(sessionId, userId).orElseThrow(() -> new NotFoundException("Workout"));
    }

    private WorkoutSession getActiveOwned(UUID userId, UUID sessionId) {
        WorkoutSession session = getOwned(userId, sessionId);
        if (!session.isActive()) {
            throw new BusinessRuleException("session_not_active", "This workout is already finished");
        }
        return session;
    }

    private static SessionExercise findExercise(WorkoutSession session, UUID id) {
        return session.getExercises().stream().filter(e -> e.getId().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("Workout exercise"));
    }

    private SessionDto flushAndMap(WorkoutSession session, UUID userId) {
        sessions.saveAndFlush(session);
        return toDto(session, userId);
    }

    private SessionDto toDto(WorkoutSession s, UUID userId) {
        List<SessionExerciseDto> exercises = s.getExercises().stream().map(e -> toDto(e, previous(userId, e)))
                .toList();
        return new SessionDto(s.getId(), s.getTitle(), s.getGym().getId(), s.getGym().getName(),
                s.getPlan() == null ? null : s.getPlan().getId(), s.getStatus(), s.getStartedAt(),
                s.getFinishedAt(), s.getNote(), exercises);
    }

    /** Ostatni zakończony wynik ćwiczenia (inny niż bieżący trening). */
    private ExerciseHistoryEntryDto previous(UUID userId, SessionExercise current) {
        return sessionExercises.findFinishedByExercise(userId, current.getExercise().getId(), PageRequest.of(0, 2))
                .stream()
                .filter(se -> !se.getSession().getId().equals(current.getSession().getId()))
                .findFirst()
                .map(WorkoutService::toHistoryEntry)
                .orElse(null);
    }

    private static SessionExerciseDto toDto(SessionExercise e, ExerciseHistoryEntryDto previous) {
        return new SessionExerciseDto(e.getId(), e.getPosition(), PlanMapper.toDto(e.getExercise()),
                PlanMapper.toDto(e.getEquipment()), e.getTargetRepsMin(), e.getTargetRepsMax(),
                e.getTargetWeightKg(), e.getRestSeconds(), e.getNote(),
                e.getSets().stream().map(s -> new SessionSetDto(s.getId(), s.getSetNumber(), s.getReps(),
                        s.getWeightKg(), s.isCompleted())).toList(),
                previous);
    }

    private static ExerciseHistoryEntryDto toHistoryEntry(SessionExercise se) {
        return new ExerciseHistoryEntryDto(se.getSession().getId(), se.getSession().getTitle(),
                se.getSession().getFinishedAt(),
                se.getSets().stream().filter(SessionSet::isCompleted)
                        .map(s -> new SetResultDto(s.getSetNumber(), s.getReps(), s.getWeightKg())).toList());
    }

    private static SessionSummaryDto toSummary(WorkoutSession s) {
        int completed = 0;
        BigDecimal volume = BigDecimal.ZERO;
        for (SessionExercise e : s.getExercises()) {
            for (SessionSet set : e.getSets()) {
                if (set.isCompleted()) {
                    completed++;
                    if (set.getReps() != null && set.getWeightKg() != null) {
                        volume = volume.add(set.getWeightKg().multiply(BigDecimal.valueOf(set.getReps())));
                    }
                }
            }
        }
        return new SessionSummaryDto(s.getId(), s.getTitle(), s.getGym().getName(), s.getStartedAt(),
                s.getFinishedAt(), s.getExercises().size(), completed, volume);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
