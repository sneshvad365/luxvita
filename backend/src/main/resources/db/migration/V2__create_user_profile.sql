CREATE TABLE user_profile (
  id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  bio              TEXT,
  goal             TEXT         NOT NULL,
  target_kcal      INT          NOT NULL,
  target_protein_g INT          NOT NULL,
  target_carbs_g   INT          NOT NULL,
  target_fat_g     INT          NOT NULL,
  target_fiber_g   INT          NOT NULL DEFAULT 25,
  target_water_l   NUMERIC(3,1) NOT NULL DEFAULT 2.5,
  base_weight_kg   NUMERIC(5,2),
  goal_weight_kg   NUMERIC(5,2),
  updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  UNIQUE(user_id)
);
