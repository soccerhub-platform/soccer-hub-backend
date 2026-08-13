--liquibase formatted sql

--changeset codex:2026-08-13-trial-confirmation-removal
UPDATE trial_bookings
SET status = 'SCHEDULED'
WHERE status = 'CONFIRMED';

DROP INDEX IF EXISTS uq_trial_booking_active_student;
DROP INDEX IF EXISTS uq_trial_booking_active_lead_participant;

CREATE UNIQUE INDEX uq_trial_booking_active_student
    ON trial_bookings (student_id)
    WHERE student_id IS NOT NULL
        AND status = 'SCHEDULED';

CREATE UNIQUE INDEX uq_trial_booking_active_lead_participant
    ON trial_bookings (lead_id, participant_id)
    WHERE lead_id IS NOT NULL
        AND participant_id IS NOT NULL
        AND status = 'SCHEDULED';