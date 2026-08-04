--liquibase formatted sql

--changeset arsen:2026-08-03-lead-participant-journey
ALTER TABLE lead_participants
    ADD COLUMN stage VARCHAR(32),
    ADD COLUMN player_id UUID,
    ADD COLUMN stage_changed_at TIMESTAMP;

ALTER TABLE lead_participants
    ADD CONSTRAINT chk_lead_participants_stage
        CHECK (
            stage IS NULL
                OR stage IN (
                             'NEW',
                             'TRIAL',
                             'CONTRACT',
                             'ENROLLMENT',
                             'FIRST_PAYMENT',
                             'COMPLETED',
                             'LOST'
                )
            );

CREATE UNIQUE INDEX uq_lead_participants_player
    ON lead_participants (player_id)
    WHERE player_id IS NOT NULL;

CREATE INDEX idx_lead_participants_stage
    ON lead_participants (stage);