-- ============================================
-- PAYMENTS MVP
-- ============================================

CREATE TABLE IF NOT EXISTS payments
(
    id                 UUID PRIMARY KEY,
    contract_id        UUID           NOT NULL,
    client_id          UUID           NOT NULL,
    player_id          UUID,
    branch_id          UUID           NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    currency           VARCHAR(10)    NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    method             VARCHAR(30)    NOT NULL,
    paid_at            TIMESTAMP      NOT NULL,
    recorded_at        TIMESTAMP      NOT NULL,
    recorded_by        UUID           NOT NULL,
    comment            TEXT,
    external_reference VARCHAR(255),
    cancel_reason      VARCHAR(255),
    cancel_comment     TEXT,
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_by         VARCHAR,
    modified_by        VARCHAR,

    CONSTRAINT fk_payments_contract
        FOREIGN KEY (contract_id)
            REFERENCES contracts (id),
    CONSTRAINT fk_payments_client
        FOREIGN KEY (client_id)
            REFERENCES client_profiles (id),
    CONSTRAINT fk_payments_player
        FOREIGN KEY (player_id)
            REFERENCES players (id),
    CONSTRAINT fk_payments_branch
        FOREIGN KEY (branch_id)
            REFERENCES branches (id),
    CONSTRAINT fk_payments_recorded_by
        FOREIGN KEY (recorded_by)
            REFERENCES app_user (id)
);

CREATE INDEX IF NOT EXISTS idx_payments_contract ON payments (contract_id);
CREATE INDEX IF NOT EXISTS idx_payments_client ON payments (client_id);
CREATE INDEX IF NOT EXISTS idx_payments_branch_paid_at ON payments (branch_id, paid_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_method ON payments (method);
