CREATE TABLE water_logs (
  id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  logged_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  amount_l  NUMERIC(4,2) NOT NULL
);

CREATE INDEX idx_water_logs_user_date ON water_logs (user_id, logged_at);
