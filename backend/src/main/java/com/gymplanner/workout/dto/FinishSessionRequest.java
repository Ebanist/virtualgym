package com.gymplanner.workout.dto;

import jakarta.validation.constraints.Size;

public record FinishSessionRequest(@Size(max = 1000) String note) {
}
