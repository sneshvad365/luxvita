CREATE INDEX idx_meals_user_logged     ON meals(user_id, logged_at DESC);
CREATE INDEX idx_activity_user_logged  ON activity_logs(user_id, logged_at DESC);
CREATE INDEX idx_weight_user_logged    ON weight_logs(user_id, logged_at DESC);
