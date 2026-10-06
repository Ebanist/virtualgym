package com.gymplanner.auth;

import java.util.UUID;

/** Zalogowany użytkownik wstrzykiwany do metod kontrolerów (rozwiązywany z tokenu JWT). */
public record AuthUser(UUID id, String email) {
}
