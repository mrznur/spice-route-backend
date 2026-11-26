
package com.spiceroute.feedback.auth;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.spiceroute.feedback.repository.RefreshTokenRepository;

@Service
public class RefreshTokenRevocationService {

    private final RefreshTokenRepository refreshTokens;

    public RefreshTokenRevocationService(
            RefreshTokenRepository refreshTokens
    ) {
        this.refreshTokens = refreshTokens;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(UUID userId) {
        refreshTokens.revokeAllForUser(userId);
    }
}
