CREATE TABLE meals (
  id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  logged_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  description  TEXT,
  has_photo    BOOLEAN      NOT NULL DEFAULT false,
  kcal         INT,
  protein_g    NUMERIC(5,1),
  carbs_g      NUMERIC(5,1),
  fat_g        NUMERIC(5,1),
  fiber_g      NUMERIC(5,1),
  raw_estimate JSONB
);
