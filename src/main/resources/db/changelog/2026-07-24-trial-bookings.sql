--liquibase formatted sql

--changeset codex:2026-07-24-trial-bookings
CREATE TABLE trial_bookings
(
    id                  UUID PRIMARY KEY,
    lead_id             UUID,
    client_id           UUID,
    student_id          UUID NOT NULL,
    training_session_id UUID NOT NULL,
    status              VARCHAR(30) NOT NULL,
    attendance_status   VARCHAR(30) NOT NULL,
    result              VARCHAR(30) NOT NULL,
    coach_feedback      TEXT,
    cancellation_reason VARCHAR(100),
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    created_by          VARCHAR,
    modified_by         VARCHAR
);

CREATE INDEX idx_trial_bookings_lead
    ON trial_bookings (lead_id);

CREATE INDEX idx_trial_bookings_client
    ON trial_bookings (client_id);

CREATE INDEX idx_trial_bookings_student
    ON trial_bookings (student_id);

CREATE INDEX idx_trial_bookings_session
    ON trial_bookings (training_session_id);

CREATE INDEX idx_trial_bookings_status
    ON trial_bookings (status);

CREATE UNIQUE INDEX uq_trial_booking_active_student_session
    ON trial_bookings (student_id, training_session_id)
    WHERE status IN ('SCHEDULED', 'CONFIRMED');
