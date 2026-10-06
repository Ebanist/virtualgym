package com.gymplanner.auth.dto;

import com.gymplanner.user.dto.UserDto;

/** Odpowiedź po zalogowaniu/odświeżeniu. Refresh token trafia wyłącznie do ciasteczka httpOnly. */
public record AuthResponse(String accessToken, long expiresIn, UserDto user) {
}
