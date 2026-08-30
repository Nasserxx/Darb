ALTER TABLE mosque_admins
  ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS left_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_mosque_admins_user_active
  ON mosque_admins (user_id) WHERE is_active = TRUE;
