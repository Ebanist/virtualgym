package com.gymplanner.user.dto;

import com.gymplanner.user.User;
import java.util.UUID;

/** Skrócona informacja o użytkowniku (autor wpisu, zgłoszenia itp.). */
public record UserRefDto(UUID id, String displayName) {

    public static UserRefDto from(User user) {
        return user == null ? null : new UserRefDto(user.getId(), user.getDisplayName());
    }
}
