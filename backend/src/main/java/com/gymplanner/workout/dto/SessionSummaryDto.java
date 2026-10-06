package com.gymplanner.workout.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** @param volumeKg suma ciężar × powtórzenia z odhaczonych serii */
public record SessionSummaryDto(UUID id, String title, String gymName, Instant startedAt, Instant finishedAt,
        int exerciseCount, int completedSets, BigDecimal volumeKg) {
}
