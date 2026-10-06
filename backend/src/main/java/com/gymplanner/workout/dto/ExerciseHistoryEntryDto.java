package com.gymplanner.workout.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Wynik ćwiczenia w jednym zakończonym treningu (tylko serie odhaczone). */
public record ExerciseHistoryEntryDto(UUID sessionId, String sessionTitle, Instant date, List<SetResultDto> sets) {
}
