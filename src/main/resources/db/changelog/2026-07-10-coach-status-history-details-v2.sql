ALTER TABLE coach_status_history
    ADD COLUMN IF NOT EXISTS vacation_from DATE,
    ADD COLUMN IF NOT EXISTS vacation_to DATE;