--liquibase formatted sql

--changeset codex:2026-07-25-trial-lead-participant
ALTER TABLE trial_bookings
    ADD COLUMN participant_id UUID;

ALTER TABLE trial_bookings
    ALTER COLUMN student_id DROP NOT NULL;

CREATE INDEX idx_trial_bookings_participant
    ON trial_bookings (participant_id);
