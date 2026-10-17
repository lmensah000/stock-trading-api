-- ============================================================
--  V5 — bot configuration and the live-trading gate
--
--  A single row holding the bot's operating mode and the condition under which
--  real money becomes reachable. One row is the point: there is one owner and
--  one gate, and a uniqueness constraint that permits exactly one row is
--  cheaper to reason about than application code that hopes there is one.
--
--  mode is PAPER until the gate is deliberately opened. No live broker adapter
--  exists yet, so LIVE currently has no execution path at all - the gate is
--  being built before the thing it guards rather than retrofitted around it.
--
--  unlock_target_equity is set once, on first boot, and never updated
--  afterwards. That immutability is the whole mechanism: a gate whose
--  threshold can be quietly lowered on the evening it would have stopped a
--  trade is decorative. Changing it has to be a visible, deliberate act
--  against the database, not a config edit.
--
--  armed and unlocked_at are separate on purpose. Reaching the target only
--  arms the gate; it does not open it. Arming is a fact the system can
--  observe, unlocking is a decision only the owner makes, and collapsing the
--  two would mean any arithmetic error in the equity calculation becomes a
--  live-money error.
-- ============================================================
CREATE TABLE bot_config (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Permits exactly one row: a constant column with a unique constraint.
    -- Guarantees singleton-ness in the schema rather than by convention.
    singleton            TINYINT NOT NULL DEFAULT 1,

    owner_user_id        BIGINT NOT NULL,

    -- PAPER or LIVE. Stored as a string via @Enumerated(EnumType.STRING) so
    -- the value is readable in the database and reordering the Java enum
    -- cannot silently change what an existing row means.
    mode                 VARCHAR(16) NOT NULL DEFAULT 'PAPER',

    -- Equity the paper account started with, kept so progress toward the
    -- target is measurable rather than just a current balance.
    starting_equity      DECIMAL(19,4) NOT NULL,

    -- Write-once. See the note above.
    unlock_target_equity DECIMAL(19,4) NOT NULL,

    -- Set when paper equity first reaches the target. Does not unlock.
    armed                TINYINT NOT NULL DEFAULT 0,
    armed_at             TIMESTAMP(6) NULL,

    -- Set only by an explicit unlock. NULL means live trading is unreachable.
    unlocked_at          TIMESTAMP(6) NULL,

    created_at           TIMESTAMP(6) NOT NULL,
    updated_at           TIMESTAMP(6) NOT NULL,

    CONSTRAINT uq_bot_config_singleton UNIQUE (singleton),

    CONSTRAINT fk_bot_config_user
        FOREIGN KEY (owner_user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
