package com.gymplanner.auth;

import com.gymplanner.auth.dto.AuthResponse;

/** Wynik operacji uwierzytelnienia: odpowiedź dla klienta + surowy refresh token do ciasteczka. */
public record AuthResult(AuthResponse response, String refreshToken) {
}
