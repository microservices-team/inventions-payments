-- ── Payments Schema ─────────────────────────────────────────────────────

CREATE SCHEMA IF NOT EXISTS payments;

-- ── User accounts (balance) ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments.user_accounts (
    user_id    VARCHAR(36)    PRIMARY KEY,   -- UUID del auth-service
    balance    NUMERIC(19,4)  NOT NULL DEFAULT 0.0000,
    currency   CHAR(3)        NOT NULL DEFAULT 'PEN',
    active     BOOLEAN        NOT NULL DEFAULT true,
    version    BIGINT         NOT NULL DEFAULT 0,  -- Optimistic locking
    created_at TIMESTAMP      DEFAULT NOW(),
    updated_at TIMESTAMP      DEFAULT NOW()
);

-- ── Write model: transactions ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments.transactions (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    command_id     VARCHAR(36)   UNIQUE NOT NULL,  -- Idempotency key
    sender_id      VARCHAR(36)   NOT NULL,
    recipient_id   VARCHAR(36)   NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    currency       CHAR(3)       NOT NULL,
    description    VARCHAR(255),
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(500),
    created_at     TIMESTAMP     DEFAULT NOW(),
    updated_at     TIMESTAMP     DEFAULT NOW(),

    CONSTRAINT chk_amount      CHECK (amount > 0),
    CONSTRAINT chk_status      CHECK (status IN ('PENDING','COMPLETED','FAILED','REVERSED')),
    CONSTRAINT chk_not_self    CHECK (sender_id <> recipient_id)
);

CREATE INDEX IF NOT EXISTS idx_tx_sender     ON payments.transactions(sender_id);
CREATE INDEX IF NOT EXISTS idx_tx_recipient  ON payments.transactions(recipient_id);
CREATE INDEX IF NOT EXISTS idx_tx_status     ON payments.transactions(status);
CREATE INDEX IF NOT EXISTS idx_tx_created    ON payments.transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tx_command    ON payments.transactions(command_id);

-- ── Read model: transaction_views (CQRS projection) ─────────────────────
CREATE TABLE IF NOT EXISTS payments.transaction_views (
    id              UUID          PRIMARY KEY,  -- Mismo UUID que transactions
    sender_id       VARCHAR(36)   NOT NULL,
    sender_name     VARCHAR(255),
    sender_email    VARCHAR(255),
    recipient_id    VARCHAR(36)   NOT NULL,
    recipient_name  VARCHAR(255),
    recipient_email VARCHAR(255),
    amount          NUMERIC(19,4) NOT NULL,
    currency        CHAR(3)       NOT NULL,
    description     VARCHAR(255),
    status          VARCHAR(20)   NOT NULL,
    failure_reason  VARCHAR(500),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tv_sender    ON payments.transaction_views(sender_id);
CREATE INDEX IF NOT EXISTS idx_tv_recipient ON payments.transaction_views(recipient_id);
CREATE INDEX IF NOT EXISTS idx_tv_status    ON payments.transaction_views(status);
CREATE INDEX IF NOT EXISTS idx_tv_created   ON payments.transaction_views(created_at DESC);
