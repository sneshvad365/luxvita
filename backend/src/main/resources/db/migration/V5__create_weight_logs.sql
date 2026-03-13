CREATE TABLE weight_logs (
  id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  logged_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  weight_kg NUMERIC(5,2) NOT NULL
);
