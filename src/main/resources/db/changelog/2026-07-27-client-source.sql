-- liquibase formatted sql

--changeset codex:2026-07-27-client-source
ALTER TABLE public.client_profiles
    ADD COLUMN IF NOT EXISTS source_details VARCHAR(500);

UPDATE public.client_profiles
SET source_details = COALESCE(NULLIF(TRIM(source_details), ''), TRIM(source))
WHERE source IS NOT NULL
  AND TRIM(source) <> ''
  AND LOWER(TRIM(source)) NOT IN (
      'lead', 'referral', 'instagram', 'whatsapp', 'website',
      'walk_in', 'walk-in', 'phone', 'call', 'partner', 'manual', 'other', 'unknown'
  );

UPDATE public.client_profiles
SET source = CASE LOWER(TRIM(source))
    WHEN 'lead' THEN 'LEAD'
    WHEN 'referral' THEN 'REFERRAL'
    WHEN 'instagram' THEN 'INSTAGRAM'
    WHEN 'whatsapp' THEN 'WHATSAPP'
    WHEN 'website' THEN 'WEBSITE'
    WHEN 'walk_in' THEN 'WALK_IN'
    WHEN 'walk-in' THEN 'WALK_IN'
    WHEN 'phone' THEN 'PHONE'
    WHEN 'call' THEN 'PHONE'
    WHEN 'partner' THEN 'PARTNER'
    WHEN 'manual' THEN 'MANUAL'
    WHEN 'other' THEN 'OTHER'
    WHEN 'unknown' THEN 'UNKNOWN'
    WHEN '' THEN 'UNKNOWN'
    ELSE 'OTHER'
END;

UPDATE public.client_profiles SET source = 'UNKNOWN' WHERE source IS NULL;

ALTER TABLE public.client_profiles ALTER COLUMN source SET DEFAULT 'UNKNOWN';

CREATE INDEX IF NOT EXISTS idx_client_profiles_source ON public.client_profiles (source);
