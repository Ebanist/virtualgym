package com.gymplanner.workout.dto;

import java.util.UUID;

/** Trening z dnia planu ({@code planId} + {@code planDayId}) albo ad hoc w siłowni ({@code gymId}). */
public record StartSessionRequest(UUID planId, UUID planDayId, UUID gymId) {
}
