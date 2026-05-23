ALTER TABLE admin_profiles
    ADD COLUMN IF NOT EXISTS specialization VARCHAR(500);

ALTER TABLE dispatcher_profiles
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS specialization VARCHAR(500),
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS profile_notification_settings
(
    user_id          UUID PRIMARY KEY,
    today_sessions   BOOLEAN NOT NULL DEFAULT TRUE,
    overdue_reports  BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_changes BOOLEAN NOT NULL DEFAULT TRUE,
    lead_reminders   BOOLEAN NOT NULL DEFAULT TRUE,
    payment_alerts   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW(),
    created_by       VARCHAR,
    modified_by      VARCHAR,

    CONSTRAINT fk_profile_notification_settings_user FOREIGN KEY (user_id)
        REFERENCES app_user(id) ON DELETE CASCADE
);
