
package com.spiceroute.feedback.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.spiceroute.feedback.domain.RefreshToken;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Delete tokens that expired before the given cutoff.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") Instant cutoff);

    /**
     * Invalidate all unused refresh tokens belonging to a user.
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken t
        SET t.used = true
        WHERE t.userId = :userId
          AND t.used = false
        """)
    void revokeAllForUser(@Param("userId") UUID userId);

    /**
     * Atomically consume a refresh token only if it has not already been used.
     *
     * @return 1 if the token was consumed, otherwise 0.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE RefreshToken t
        SET t.used = true
        WHERE t.id = :id
          AND t.used = false
        """)
    int consumeIfUnused(@Param("id") UUID id);
}
