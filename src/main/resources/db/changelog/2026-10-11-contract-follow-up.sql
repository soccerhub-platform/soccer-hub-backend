-- liquibase formatted sql
-- changeset codex:2026-10-11-contract-follow-up
CREATE TABLE contract_follow_ups (
 contract_id UUID PRIMARY KEY REFERENCES contracts(id), owner_id UUID REFERENCES admin_profiles(id), next_action VARCHAR(240), due_at TIMESTAMPTZ,
 outcome VARCHAR(40), promised_payment_date DATE, last_contact_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE contract_follow_up_history (
 id UUID PRIMARY KEY, contract_id UUID NOT NULL REFERENCES contracts(id),actor_id UUID NOT NULL REFERENCES app_user(id),
 type VARCHAR(40) NOT NULL, description TEXT NOT NULL, occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX contract_follow_up_due_idx ON contract_follow_ups(due_at) WHERE next_action IS NOT NULL;
CREATE INDEX contract_follow_up_history_idx ON contract_follow_up_history(contract_id,occurred_at);

-- changeset codex:2026-10-11-debt-snapshots
CREATE TABLE payment_debt_snapshots (
 branch_id UUID NOT NULL, snapshot_date DATE NOT NULL,currency VARCHAR(10) NOT NULL,
 contract_count BIGINT NOT NULL,amount NUMERIC(19,2) NOT NULL,
 PRIMARY KEY(branch_id,snapshot_date,currency)
);

-- changeset codex:2026-10-11-trial-workflow
ALTER TABLE trial_bookings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE trial_operation_history (
 id UUID PRIMARY KEY,trial_id UUID NOT NULL REFERENCES trial_bookings(id),actor_id UUID NOT NULL REFERENCES app_user(id),
 occurred_at TIMESTAMPTZ NOT NULL,type VARCHAR(40) NOT NULL,description TEXT NOT NULL
);
CREATE INDEX trial_operation_history_idx ON trial_operation_history(trial_id,occurred_at);

-- changeset codex:2026-10-11-lead-workflow-actor
-- Lead actions may now be performed by an administrator or a dispatcher.
ALTER TABLE lead_activities DROP CONSTRAINT IF EXISTS fk_lead_activities_actor_admin;
ALTER TABLE lead_activities ADD CONSTRAINT fk_lead_activities_actor_user FOREIGN KEY(actor_admin_id) REFERENCES app_user(id);
