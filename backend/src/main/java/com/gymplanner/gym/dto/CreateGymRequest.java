package com.gymplanner.gym.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param confirmDuplicate {@code true}, gdy użytkownik widział ostrzeżenie o podobnych siłowniach i mimo to
 *                         chce dodać nową.
 */
public record CreateGymRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Size(min = 2, max = 80) String city,
        @NotBlank @Size(min = 3, max = 200) String address,
        @Size(max = 2000) String description,
        boolean confirmDuplicate) {
}
