package com.gymplanner.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Unieważnia sesję (wylogowanie, wykryta kradzież tokenu). */
    @Modifying
    @Query("delete from RefreshToken t where t.familyId = :familyId")
    int deleteFamily(UUID familyId);

    /** Unieważnia wszystkie sesje użytkownika (zmiana/reset hasła). */
    @Modifying
    @Query("delete from RefreshToken t where t.user.id = :userId")
    int deleteAllForUser(UUID userId);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    int deleteExpired(Instant now);
}
