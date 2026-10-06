package com.gymplanner.exercise;

import com.gymplanner.auth.AuthUser;
import com.gymplanner.exercise.dto.AvailableExerciseDto;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.dto.EquipmentExerciseDto;
import com.gymplanner.exercise.dto.ExerciseDto;
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

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping("/exercises")
    @Operation(summary = "Biblioteka ćwiczeń: globalne + własne ćwiczenia moich siłowni")
    public List<ExerciseDto> library(AuthUser user, @RequestParam(required = false) String q,
            @RequestParam(required = false) MuscleGroup muscle) {
        return exerciseService.library(user.id(), q, muscle);
    }

    @GetMapping("/exercises/{id}")
    public ExerciseDto get(AuthUser user, @PathVariable UUID id) {
        return exerciseService.get(user.id(), id);
    }

    @GetMapping("/gyms/{gymId}/exercises/available")
    @Operation(summary = "Ćwiczenia możliwe do wykonania na sprzęcie siłowni (+ z masą ciała) wraz ze sprzętem")
    public List<AvailableExerciseDto> available(@PathVariable UUID gymId, @RequestParam(required = false) String q,
            @RequestParam(required = false) MuscleGroup muscle) {
        return exerciseService.available(gymId, q, muscle);
    }

    @PostMapping("/gyms/{gymId}/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Własne ćwiczenie przypisane do sprzętu siłowni (tylko członkowie)")
    public ExerciseDto createCustom(AuthUser user, @PathVariable UUID gymId,
            @Valid @RequestBody CreateExerciseRequest request) {
        return exerciseService.createCustom(user.id(), gymId, request);
    }

    @GetMapping("/equipment/{equipmentId}/exercises")
    @Operation(summary = "Ćwiczenia na danym sprzęcie (dopasowanie po typie lub jawne powiązanie)")
    public List<EquipmentExerciseDto> forEquipment(@PathVariable UUID equipmentId) {
        return exerciseService.forEquipment(equipmentId);
    }

    @PutMapping("/equipment/{equipmentId}/exercises/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Powiązanie sprzętu z ćwiczeniem (idempotentne, członkowie)")
    public void link(AuthUser user, @PathVariable UUID equipmentId, @PathVariable UUID exerciseId) {
        exerciseService.link(user.id(), equipmentId, exerciseId);
    }

    @DeleteMapping("/equipment/{equipmentId}/exercises/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(AuthUser user, @PathVariable UUID equipmentId, @PathVariable UUID exerciseId) {
        exerciseService.unlink(user.id(), equipmentId, exerciseId);
    }
}
