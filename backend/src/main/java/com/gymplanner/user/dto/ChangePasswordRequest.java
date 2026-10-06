package com.gymplanner.user.dto;

import com.gymplanner.auth.dto.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String newPassword) {
}
