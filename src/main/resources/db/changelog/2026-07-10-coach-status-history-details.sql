-- liquibase formatted sql

-- changeset soccerhub:2026-07-10-coach-status-history-details
ALTER TABLE coach_status_history
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS previous_account_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS new_account_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS previous_work_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS new_work_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS reason VARCHAR(500);
