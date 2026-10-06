package com.gymplanner.auth;

import com.gymplanner.auth.dto.AuthResponse;
import com.gymplanner.auth.dto.LoginRequest;
import com.gymplanner.auth.dto.PasswordResetConfirmRequest;
import com.gymplanner.auth.dto.RegisterRequest;
import com.gymplanner.common.error.BusinessRuleException;
import com.gymplanner.common.error.ConflictException;
import com.gymplanner.common.error.UnauthorizedException;
import com.gymplanner.config.AppProperties;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import com.gymplanner.user.dto.UserDto;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties properties;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
            PasswordResetTokenRepository resetTokens, PasswordEncoder passwordEncoder, JwtService jwtService,
            AppProperties properties, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("timing-attack-protection");
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("email_taken", "Email is already registered");
        }
        User user = users.save(new User(email, passwordEncoder.encode(request.password()),
                request.displayName().trim()));
        return issueTokens(user, UUID.randomUUID());
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = users.findByEmail(normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issueTokens(user, UUID.randomUUID());
    }

    /**
     * Rotacja refresh tokenu. Ponowne użycie już zrotowanego tokenu w krótkim oknie tolerancji
     * (równoległe odświeżenie z kilku kart) wydaje nowy token; po oknie – usuwa całą sesję.
     */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("refresh_token_missing", "Refresh token missing");
        }
        Instant now = clock.instant();
        RefreshToken token = refreshTokens.findByTokenHash(SecureTokens.hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("refresh_token_invalid", "Invalid refresh token"));
        if (!now.isBefore(token.getExpiresAt())) {
            throw new UnauthorizedException("refresh_token_expired", "Refresh token expired");
        }
        if (token.isRotated()) {
            Instant graceEnd = token.getRotatedAt().plus(properties.jwt().refreshReuseGrace());
            if (!now.isBefore(graceEnd)) {
                log.warn("Reuse of rotated refresh token detected for user {}", token.getUser().getId());
                refreshTokens.deleteFamily(token.getFamilyId());
                throw new UnauthorizedException("refresh_token_invalid", "Invalid refresh token");
            }
        }
        token.markRotated(now);
        return issueTokens(token.getUser(), token.getFamilyId());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(SecureTokens.hash(rawToken))
                .ifPresent(t -> refreshTokens.deleteFamily(t.getFamilyId()));
    }

    /** Unieważnia wszystkie sesje użytkownika i otwiera nową (np. po zmianie hasła). */
    @Transactional
    public AuthResult reissueAfterPasswordChange(User user) {
        refreshTokens.deleteAllForUser(user.getId());
        return issueTokens(user, UUID.randomUUID());
    }

    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            String raw = SecureTokens.generate();
            Instant expiresAt = clock.instant().plus(properties.jwt().passwordResetTtl());
            resetTokens.save(new PasswordResetToken(user, SecureTokens.hash(raw), expiresAt));
            // MVP: brak wysyłki e-maili – link trafia do logów backendu.
            log.info("Password reset link for {}: {}/reset-password?token={}", user.getEmail(),
                    properties.frontendUrl(), raw);
        });
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        Instant now = clock.instant();
        PasswordResetToken token = resetTokens.findByTokenHash(SecureTokens.hash(request.token()))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new BusinessRuleException("reset_token_invalid",
                        "Password reset link is invalid or expired"));
        token.markUsed(now);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokens.deleteAllForUser(user.getId());
    }

    /** Sprzątanie przeterminowanych refresh tokenów (raz na dobę). */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int removed = refreshTokens.deleteExpired(clock.instant());
        log.info("Purged {} expired refresh tokens", removed);
    }

    private AuthResult issueTokens(User user, UUID familyId) {
        String raw = SecureTokens.generate();
        Instant expiresAt = clock.instant().plus(properties.jwt().refreshTokenTtl());
        refreshTokens.save(new RefreshToken(user, SecureTokens.hash(raw), familyId, expiresAt));
        AuthResponse response = new AuthResponse(jwtService.createAccessToken(user),
                jwtService.accessTokenTtlSeconds(), UserDto.from(user));
        return new AuthResult(response, raw);
    }

    private static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("invalid_credentials", "Invalid email or password");
    }
}
