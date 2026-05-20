-- ============================================
-- LEAD LOSS REASONS
-- ============================================

CREATE TABLE IF NOT EXISTS lead_loss_reasons
(
    code       VARCHAR(64) PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO lead_loss_reasons (code, name, active, sort_order)
VALUES
    ('PRICE', 'Цена', TRUE, 10),
    ('SCHEDULE_NOT_SUITABLE', 'Не подходит расписание', TRUE, 20),
    ('LOCATION_NOT_SUITABLE', 'Не подходит локация', TRUE, 30),
    ('NO_RESPONSE', 'Не отвечает', TRUE, 40),
    ('CHOOSE_COMPETITOR', 'Выбрали конкурента', TRUE, 50),
    ('CHANGED_MIND', 'Передумали', TRUE, 60),
    ('CHILD_NOT_INTERESTED', 'Ребёнку не интересно', TRUE, 70),
    ('PARENT_NOT_READY', 'Родитель пока не готов', TRUE, 80),
    ('MEDICAL_REASON', 'Медицинская причина', TRUE, 90),
    ('OTHER', 'Другое', TRUE, 100)
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- LEADS LOSS SNAPSHOT
-- ============================================

ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS lost_reason_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS lost_comment TEXT,
    ADD COLUMN IF NOT EXISTS lost_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_leads_lost_reason_code'
    ) THEN
        ALTER TABLE leads
            ADD CONSTRAINT fk_leads_lost_reason_code
                FOREIGN KEY (lost_reason_code)
                    REFERENCES lead_loss_reasons (code);
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_leads_lost_reason_code ON leads(lost_reason_code);
CREATE INDEX IF NOT EXISTS idx_leads_lost_at ON leads(lost_at);
