-- ============================================
-- CONTRACTS ADMIN API SUPPORT
-- ============================================

ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS contract_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS lead_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS coach_id UUID,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10),
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS cancel_reason_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS cancel_comment TEXT;

UPDATE contracts
SET lead_type = COALESCE(lead_type, 'CHILDREN');

UPDATE contracts
SET status = COALESCE(
        status,
        CASE
            WHEN end_date IS NOT NULL AND end_date < CURRENT_DATE THEN 'EXPIRED'
            WHEN start_date > CURRENT_DATE THEN 'UPCOMING'
            ELSE 'ACTIVE'
            END
             );

UPDATE contracts
SET currency = COALESCE(currency, 'KZT');

UPDATE contracts
SET contract_number = COALESCE(
        contract_number,
        'CNT-' || EXTRACT(YEAR FROM COALESCE(start_date, CURRENT_DATE))::TEXT || '-' ||
        UPPER(SUBSTRING(REPLACE(id::TEXT, '-', '') FROM 1 FOR 5))
                      );

ALTER TABLE contracts
    ALTER COLUMN contract_number SET NOT NULL,
    ALTER COLUMN lead_type SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN currency SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_contracts_contract_number ON contracts (contract_number);
CREATE INDEX IF NOT EXISTS idx_contracts_status ON contracts (status);
CREATE INDEX IF NOT EXISTS idx_contracts_lead_type ON contracts (lead_type);
CREATE INDEX IF NOT EXISTS idx_contracts_player_status_dates ON contracts (player_id, status, start_date, end_date);

CREATE TABLE IF NOT EXISTS contract_cancel_reasons
(
    code        VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO contract_cancel_reasons (code, description, active)
VALUES ('CLIENT_REQUEST', 'Client requested cancellation', TRUE),
       ('PAYMENT_ISSUE', 'Payment issue', TRUE),
       ('SCHEDULE_CONFLICT', 'Schedule conflict', TRUE),
       ('MEDICAL', 'Medical reason', TRUE),
       ('OTHER', 'Other reason', TRUE)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS contract_history
(
    id            UUID PRIMARY KEY,
    contract_id   UUID         NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    actor_user_id UUID,
    actor_name    VARCHAR(255) NOT NULL,
    comment       TEXT,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW(),
    created_by    VARCHAR,
    modified_by   VARCHAR,

    CONSTRAINT fk_contract_history_contract
        FOREIGN KEY (contract_id)
            REFERENCES contracts (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_contract_history_contract_created_at
    ON contract_history (contract_id, created_at DESC);
