package com.gymplanner.auth;

import com.gymplanner.auth.dto.AuthResponse;
import com.gymplanner.auth.dto.LoginRequest;
import com.gymplanner.auth.dto.PasswordResetConfirmRequest;
import com.gymplanner.auth.dto.PasswordResetRequest;
import com.gymplanner.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookies cookies;

    public AuthController(AuthService authService, RefreshTokenCookies cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @Operation(summary = "Rejestracja nowego konta (loguje od razu)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withCookie(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withCookie(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Nowy access token na podstawie refresh tokenu z ciasteczka (rotacja)")
    public ResponseEntity<AuthResponse> refresh(
            @Parameter(hidden = true) @CookieValue(name = RefreshTokenCookies.NAME, required = false) String token) {
        return withCookie(HttpStatus.OK, authService.refresh(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @CookieValue(name = RefreshTokenCookies.NAME, required = false) String token) {
        authService.logout(token);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.clear()).build();
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Żądanie resetu hasła (w MVP link jest logowany w konsoli backendu)")
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
    }

    private ResponseEntity<AuthResponse> withCookie(HttpStatus status, AuthResult result) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookies.create(result.refreshToken()))
                .body(result.response());
    }
}
