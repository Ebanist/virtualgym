package com.gymplanner.auth;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token (przechowywany jako hash SHA-256). Tokeny z jednej sesji logowania dzielą {@code familyId}.
 * {@code rotatedAt} oznacza token wymieniony na nowy; ponowne użycie takiego tokenu (po oknie tolerancji)
 * usuwa całą rodzinę (wykrycie kradzieży). Unieważnienie sesji = usunięcie wierszy.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant rotatedAt;

    protected RefreshToken() {
    }

    public RefreshToken(User user, String tokenHash, UUID familyId, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }

    public boolean isRotated() {
        return rotatedAt != null;
    }

    public void markRotated(Instant now) {
        if (rotatedAt == null) {
            rotatedAt = now;
        }
    }
}
