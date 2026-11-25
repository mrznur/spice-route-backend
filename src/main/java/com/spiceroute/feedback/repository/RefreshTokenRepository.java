package com.spiceroute.feedback.repository;

import com.spiceroute.feedback.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Housekeeping: delete tokens that expired before the given cutoff. */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") Instant cutoff);

    /** Invalidate all tokens for a user (e.g. on password change or account suspension). */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.used = true WHERE t.userId = :userId AND t.used = false")
    void revokeAllForUser(@Param("userId") UUID userId);
}
