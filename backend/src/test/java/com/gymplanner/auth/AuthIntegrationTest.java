package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.support.AbstractIntegrationTest;
import com.gymplanner.support.TestUsers;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerReturnsTokensAndHttpOnlyRefreshCookie() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "New.User-" + suffix + "@Example.com", "password", "Secret123",
                                "displayName", "Nowy"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("new.user-" + suffix + "@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email().toUpperCase(), "password", "Secret123",
                                "displayName", "Dup"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"));
    }

    @Test
    void registerValidatesInputAsProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "not-an-email", "password", "short", "displayName", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/problem+json")))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("displayName")));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email(), "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    void loginAndAccessProtectedEndpoint() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email(), "password", TestUsers.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        String token = body(login).get("accessToken").asText();

        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.email()));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401ProblemDetail() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndDetectsReuse() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);

        MvcResult refreshed = mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookies.NAME, user.refreshCookie())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String rotated = refreshed.getResponse().getCookie(RefreshTokenCookies.NAME).getValue();
        assertThat(rotated).isNotEqualTo(user.refreshCookie());

        // Ponowne użycie starego tokenu => 401 i unieważnienie całej rodziny (również nowego tokenu).
        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookies.NAME, user.refreshCookie())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(RefreshTokenCookies.NAME, rotated)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieReturns401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("refresh_token_missing"));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        Cookie cookie = new Cookie(RefreshTokenCookies.NAME, user.refreshCookie());
        mvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        mvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfileAndChangePassword() throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        mvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("displayName", "Zmieniony"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Zmieniony"));

        mvc.perform(post("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "WrongPass1", "newPassword", "NewSecret456"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("invalid_current_password"));

        mvc.perform(post("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", TestUsers.PASSWORD,
                                "newPassword", "NewSecret456"))))
                .andExpect(status().isOk());

        // Stary refresh token unieważniony, nowe hasło działa.
        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookies.NAME, user.refreshCookie())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email(), "password", "NewSecret456"))))
                .andExpect(status().isOk());
    }

    @Test
    void passwordResetWithInvalidTokenFails() throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@example.com"))))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", "nope", "newPassword", "NewSecret456"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("reset_token_invalid"));
    }
}
