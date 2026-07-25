--liquibase formatted sql

--changeset codex:2026-07-24-trial-booking-lifecycle
ALTER TABLE trial_bookings
    ADD COLUMN confirmed_at TIMESTAMP,
    ADD COLUMN canceled_at TIMESTAMP,
    ADD COLUMN completed_at TIMESTAMP,
    ADD COLUMN attendance_marked_at TIMESTAMP,
    ADD COLUMN attendance_marked_by UUID,
    ADD COLUMN attendance_comment TEXT,
    ADD COLUMN recommended_group_id UUID,
    ADD COLUMN next_action_type VARCHAR(100),
    ADD COLUMN next_action_at TIMESTAMP;
