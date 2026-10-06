-- ============================================================
--  V4 — one-time-use refresh tokens
--
--  Before this migration /api/auth/refresh issued a new token pair without
--  retiring the token presented to it. A refresh token therefore stayed valid
--  for its full seven-day TTL no matter how many times it was redeemed, so a
--  stolen one could be redeemed indefinitely alongside the legitimate holder
--  and neither side would see a sign of the other.
--
--  One row is written per issued refresh token. The token itself is never
--  stored - only its jti, which is a random UUID carried in the JWT's "jti"
--  claim and is not a credential. The signature still proves authenticity;
--  this table only answers "has this one already been spent".
--
--  used_at is the gate. Redemption is a conditional UPDATE that requires
--  used_at IS NULL, so InnoDB's row lock decides the winner when two requests
--  present the same token at once and exactly one can proceed.
--
--  revoked_at exists because a replayed token means the token leaked, and the
--  holder cannot be told apart from the thief. Per the OAuth 2.0 security best
--  current practice the whole family is then revoked rather than just the one
--  redemption refused, which forces a fresh login and ends the thief's access.
-- ============================================================
CREATE TABLE refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- The JWT "jti" claim. UUID text, so CHAR(36) is exact rather than padded.
    jti             CHAR(36) NOT NULL UNIQUE,

    user_id         BIGINT NOT NULL,

    issued_at       TIMESTAMP(6) NOT NULL,

    -- Mirrors the JWT's own exp. Duplicated here so the purge job can find
    -- dead rows without parsing tokens, and so redemption can reject an
    -- expired row even if the signature still verifies.
    expires_at      TIMESTAMP(6) NOT NULL,

    -- NULL until redeemed. Set exactly once; see the note on the gate above.
    used_at         TIMESTAMP(6) NULL,

    -- Set when the family is revoked after a replay, or on logout.
    revoked_at      TIMESTAMP(6) NULL,

    -- The jti issued in exchange for this one. Audit only: it makes a replay
    -- traceable back through the chain to the login that started it.
    replaced_by_jti CHAR(36) NULL,

    KEY idx_refresh_tokens_user (user_id),

    -- The purge job scans by expiry.
    KEY idx_refresh_tokens_expires (expires_at),

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
