package com.gymplanner.auth;

import com.gymplanner.config.AppProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh token trzymamy w ciasteczku httpOnly ograniczonym do ścieżki /api/v1/auth i SameSite=Strict –
 * niedostępny dla JS (XSS) i niewysyłany z obcych stron (CSRF).
 */
@Component
public class RefreshTokenCookies {

    public static final String NAME = "gp_refresh";
    private static final String PATH = "/api/v1/auth";

    private final AppProperties properties;

    public RefreshTokenCookies(AppProperties properties) {
        this.properties = properties;
    }

    public String create(String rawToken) {
        return build(rawToken, properties.jwt().refreshTokenTtl());
    }

    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.jwt().secureCookie())
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
