CREATE UNIQUE INDEX IF NOT EXISTS uq_training_sessions_group_slot_active
    ON training_sessions (group_id, session_date, scheduled_start_at)
    WHERE status <> 'CANCELLED';
