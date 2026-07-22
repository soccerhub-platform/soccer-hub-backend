-- liquibase formatted sql

--changeset codex:2026-07-22-client-profile-identity
ALTER TABLE public.client_profiles
    ADD COLUMN IF NOT EXISTS user_id uuid,
    ADD COLUMN IF NOT EXISTS email varchar(255);

UPDATE public.client_profiles
SET user_id = id
WHERE user_id IS NULL
  AND EXISTS (
    SELECT 1 FROM public.app_user user_account WHERE user_account.id = client_profiles.id
);

UPDATE public.client_profiles
SET email = trim(comments)
WHERE email IS NULL
  AND trim(comments) ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$';

ALTER TABLE public.client_profiles
    DROP CONSTRAINT IF EXISTS fk_client_profiles_user;

ALTER TABLE public.client_profiles
    ADD CONSTRAINT fk_client_profiles_user_account
        FOREIGN KEY (user_id) REFERENCES public.app_user (id) ON DELETE SET NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_client_profiles_user_id
    ON public.client_profiles (user_id)
    WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_client_profiles_email
    ON public.client_profiles (lower(email));
