package com.spiceroute.feedback.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId()            { return id; }
    public String getTokenHash()   { return tokenHash; }
    public void setTokenHash(String h) { this.tokenHash = h; }
    public UUID getUserId()        { return userId; }
    public void setUserId(UUID id) { this.userId = id; }
    public Instant getExpiresAt()  { return expiresAt; }
    public void setExpiresAt(Instant e) { this.expiresAt = e; }
    public boolean isUsed()        { return used; }
    public void setUsed(boolean u) { this.used = u; }
    public Instant getCreatedAt()  { return createdAt; }
}
