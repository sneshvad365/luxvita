CREATE TABLE weekly_insights (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  week_date  DATE NOT NULL,
  insight    TEXT NOT NULL,
  type       TEXT NOT NULL,
  rank       INT  NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, week_date, rank)
);
