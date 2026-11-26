-- Refresh token rotation: each token is single-use.
-- A used or expired token is rejected; a reused token invalidates the whole family (security event).

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,   -- SHA-256 hex of the raw JWT string
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_hash    ON refresh_tokens (token_hash);
-- Periodic cleanup target: tokens expired more than 1 day ago are safe to delete
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);
