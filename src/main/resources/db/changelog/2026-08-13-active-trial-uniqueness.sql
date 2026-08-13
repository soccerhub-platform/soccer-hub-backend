--liquibase formatted sql

--changeset codex:2026-08-13-active-trial-uniqueness
DROP INDEX IF EXISTS uq_trial_booking_active_student_session;

CREATE UNIQUE INDEX uq_trial_booking_active_student
    ON trial_bookings (student_id)
    WHERE student_id IS NOT NULL
        AND status IN ('SCHEDULED', 'CONFIRMED');

CREATE UNIQUE INDEX uq_trial_booking_active_lead_participant
    ON trial_bookings (lead_id, participant_id)
    WHERE lead_id IS NOT NULL
        AND participant_id IS NOT NULL
        AND status IN ('SCHEDULED', 'CONFIRMED');