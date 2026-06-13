ALTER TABLE leads RENAME COLUMN parent_name TO primary_contact_name;
ALTER TABLE leads RENAME COLUMN phone TO primary_contact_phone;
ALTER TABLE leads RENAME COLUMN email TO primary_contact_email;

ALTER TABLE leads ADD COLUMN lead_type VARCHAR(20) NOT NULL DEFAULT 'CHILDREN';
ALTER TABLE leads ADD COLUMN time_preference VARCHAR(20);

ALTER TABLE leads RENAME COLUMN player_id TO participant_id;

ALTER TABLE lead_children RENAME TO lead_participants;
ALTER TABLE lead_participants RENAME COLUMN child_name TO full_name;
ALTER TABLE lead_participants RENAME COLUMN child_age TO legacy_age;
ALTER TABLE lead_participants ADD COLUMN birth_date DATE;
ALTER TABLE lead_participants DROP COLUMN legacy_age;

ALTER TABLE lead_trials RENAME COLUMN child_id TO participant_id;

ALTER TABLE groups ADD COLUMN audience_type VARCHAR(20) NOT NULL DEFAULT 'CHILDREN';
