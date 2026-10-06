package com.gymplanner.gym.dto;

import com.gymplanner.gym.GymStatus;
import java.util.UUID;

public record GymSummaryDto(UUID id, String name, String city, String address, GymStatus status, long memberCount,
        boolean member) {
}
