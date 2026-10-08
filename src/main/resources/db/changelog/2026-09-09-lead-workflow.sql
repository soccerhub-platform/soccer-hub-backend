--liquibase formatted sql
--changeset codex:2026-09-09-lead-workflow
ALTER TABLE leads ADD COLUMN work_priority varchar(16) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE leads ADD COLUMN next_action varchar(240);
ALTER TABLE leads ADD COLUMN next_action_at timestamptz;
ALTER TABLE leads ADD COLUMN last_contact_at timestamptz;
ALTER TABLE leads ADD COLUMN stage_changed_at timestamptz;
ALTER TABLE leads ADD COLUMN work_version bigint NOT NULL DEFAULT 0;
CREATE INDEX idx_leads_branch_next_action ON leads (branch_id, next_action_at) WHERE next_action_at IS NOT NULL;
--rollback DROP INDEX idx_leads_branch_next_action;
--rollback ALTER TABLE leads DROP COLUMN work_priority, DROP COLUMN next_action, DROP COLUMN next_action_at, DROP COLUMN last_contact_at, DROP COLUMN stage_changed_at, DROP COLUMN work_version;
