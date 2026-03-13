CREATE TABLE activity_logs (
  id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  logged_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  entry     TEXT        NOT NULL,
  parsed    JSONB
);
