-- ============================================================
--  V3 — OAuth token storage for outbound market-data providers
--
--  The refresh token has to survive a restart: Schwab refresh tokens are
--  one-time use and last seven days, so losing the stored copy means the
--  integration cannot recover without a human completing a browser login.
--
--  refresh_token is a credential and is stored encrypted. It is never logged
--  and is never returned over any API.
--
--  token_version backs the compare-and-set that keeps refresh single-flight.
--  A scan runs one task per ticker, so when the access token expires every
--  in-flight task sees 401 at once. Because each refresh consumes the stored
--  token and issues a replacement, concurrent refreshes race to spend the same
--  one-time value and all but one fail - which strands the integration. The
--  version column lets exactly one writer win.
-- ============================================================
CREATE TABLE oauth_tokens (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider                VARCHAR(32) NOT NULL UNIQUE,

    -- Encrypted at rest. Length allows for the ciphertext envelope.
    refresh_token_encrypted VARBINARY(2048) NOT NULL,

    -- Access tokens are short-lived (30 minutes) and held in memory only;
    -- only the expiry is persisted so a restart knows to refresh immediately.
    access_token_expires_at TIMESTAMP(6) NULL,

    -- When the current refresh token was issued. Its seven-day expiry is
    -- measured from here, so the age can be alerted on before it strands a
    -- scheduled scan over a weekend.
    refresh_token_issued_at TIMESTAMP(6) NOT NULL,

    token_version           BIGINT NOT NULL DEFAULT 0,
    updated_at              TIMESTAMP(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
