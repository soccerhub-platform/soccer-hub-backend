ALTER TABLE coach_profiles
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(20);

ALTER TABLE coach_profiles
    ADD COLUMN IF NOT EXISTS work_status VARCHAR(20);

ALTER TABLE coach_profiles
    ADD COLUMN IF NOT EXISTS vacation_from DATE;

ALTER TABLE coach_profiles
    ADD COLUMN IF NOT EXISTS vacation_to DATE;

ALTER TABLE coach_profiles
    ADD COLUMN IF NOT EXISTS work_status_reason VARCHAR(255);

UPDATE coach_profiles
SET account_status = CASE
    WHEN status = 'INACTIVE' THEN 'INACTIVE'
    ELSE 'ACTIVE'
END
WHERE account_status IS NULL;

UPDATE coach_profiles
SET work_status = CASE
    WHEN status = 'BUSY' THEN 'BUSY'
    WHEN status = 'VACATION' THEN 'VACATION'
    ELSE 'AVAILABLE'
END
WHERE work_status IS NULL;

ALTER TABLE coach_profiles
    ALTER COLUMN account_status SET NOT NULL;

ALTER TABLE coach_profiles
    ALTER COLUMN work_status SET NOT NULL;

ALTER TABLE coach_profiles
    ALTER COLUMN status DROP NOT NULL;

DROP INDEX IF EXISTS idx_coach_profiles_status;

CREATE INDEX IF NOT EXISTS idx_coach_profiles_account_status
    ON coach_profiles(account_status);

CREATE INDEX IF NOT EXISTS idx_coach_profiles_work_status
    ON coach_profiles(work_status);
