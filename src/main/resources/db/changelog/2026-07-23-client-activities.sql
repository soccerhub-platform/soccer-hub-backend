-- liquibase formatted sql

--changeset codex:2026-07-23-client-activities
CREATE TABLE IF NOT EXISTS public.client_activities
(
    id uuid PRIMARY KEY,
    client_id uuid NOT NULL REFERENCES public.client_profiles (id) ON DELETE CASCADE,
    activity_type varchar(64) NOT NULL,
    actor_user_id uuid REFERENCES public.app_user (id),
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    occurred_at timestamp DEFAULT now() NOT NULL,
    created_at timestamp DEFAULT now() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_client_activities_client_occurred
    ON public.client_activities (client_id, occurred_at DESC);
