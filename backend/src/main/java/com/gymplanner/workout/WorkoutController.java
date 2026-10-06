package com.gymplanner.workout;

import com.gymplanner.auth.AuthUser;
import com.gymplanner.common.web.PageResponse;
import com.gymplanner.workout.dto.AddSessionExerciseRequest;
import com.gymplanner.workout.dto.ExerciseHistoryEntryDto;
import com.gymplanner.workout.dto.FinishSessionRequest;
import com.gymplanner.workout.dto.SessionDto;
import com.gymplanner.workout.dto.SessionSummaryDto;
import com.gymplanner.workout.dto.StartSessionRequest;
import com.gymplanner.workout.dto.UpdateSetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tryb treningu i historia. Operacje na treningu zwracają cały trening (z poprzednimi wynikami). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Workouts")
@Validated
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start treningu z dnia planu lub ad hoc; 409 session_already_active (activeSessionId)")
    public SessionDto start(AuthUser user, @RequestBody StartSessionRequest request) {
        return workoutService.start(user.id(), request);
    }

    @GetMapping("/sessions/active")
    @Operation(summary = "Trwający trening (204, jeśli brak)")
    public ResponseEntity<SessionDto> active(AuthUser user) {
        return workoutService.active(user.id()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/sessions")
    @Operation(summary = "Historia zakończonych treningów (od najnowszych)")
    public PageResponse<SessionSummaryDto> history(AuthUser user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return workoutService.history(user.id(), page, size);
    }

    @GetMapping("/sessions/{sessionId}")
    public SessionDto get(AuthUser user, @PathVariable UUID sessionId) {
        return workoutService.get(user.id(), sessionId);
    }

    @PatchMapping("/sessions/{sessionId}/sets/{setId}")
    @Operation(summary = "Zapis serii: powtórzenia, ciężar, odhaczenie")
    public SessionDto updateSet(AuthUser user, @PathVariable UUID sessionId, @PathVariable UUID setId,
            @Valid @RequestBody UpdateSetRequest request) {
        return workoutService.updateSet(user.id(), sessionId, setId, request);
    }

    @DeleteMapping("/sessions/{sessionId}/sets/{setId}")
    public SessionDto removeSet(AuthUser user, @PathVariable UUID sessionId, @PathVariable UUID setId) {
        return workoutService.removeSet(user.id(), sessionId, setId);
    }

    @PostMapping("/sessions/{sessionId}/exercises/{sessionExerciseId}/sets")
    @Operation(summary = "Dodatkowa seria")
    public SessionDto addSet(AuthUser user, @PathVariable UUID sessionId, @PathVariable UUID sessionExerciseId) {
        return workoutService.addSet(user.id(), sessionId, sessionExerciseId);
    }

    @PostMapping("/sessions/{sessionId}/exercises")
    @Operation(summary = "Dodanie ćwiczenia do treningu (dostępnego w siłowni)")
    public SessionDto addExercise(AuthUser user, @PathVariable UUID sessionId,
            @Valid @RequestBody AddSessionExerciseRequest request) {
        return workoutService.addExercise(user.id(), sessionId, request);
    }

    @DeleteMapping("/sessions/{sessionId}/exercises/{sessionExerciseId}")
    public SessionDto removeExercise(AuthUser user, @PathVariable UUID sessionId,
            @PathVariable UUID sessionExerciseId) {
        return workoutService.removeExercise(user.id(), sessionId, sessionExerciseId);
    }

    @PostMapping("/sessions/{sessionId}/finish")
    @Operation(summary = "Zakończenie i zapis treningu do historii")
    public SessionDto finish(AuthUser user, @PathVariable UUID sessionId,
            @Valid @RequestBody(required = false) FinishSessionRequest request) {
        return workoutService.finish(user.id(), sessionId, request == null ? null : request.note());
    }

    @PostMapping("/sessions/{sessionId}/abandon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Porzucenie treningu (nie trafia do historii)")
    public void abandon(AuthUser user, @PathVariable UUID sessionId) {
        workoutService.abandon(user.id(), sessionId);
    }

    @GetMapping("/exercises/{exerciseId}/history")
    @Operation(summary = "Ostatnie wyniki ćwiczenia z zakończonych treningów")
    public List<ExerciseHistoryEntryDto> exerciseHistory(AuthUser user, @PathVariable UUID exerciseId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
        return workoutService.exerciseHistory(user.id(), exerciseId, limit);
    }
}
