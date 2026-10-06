package com.gymplanner.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String frontendUrl,
        @NotNull Jwt jwt,
        @NotNull Cors cors) {

    public record Jwt(
            @NotBlank String secret,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl,
            @NotNull Duration refreshReuseGrace,
            @NotNull Duration passwordResetTtl,
            boolean secureCookie) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
