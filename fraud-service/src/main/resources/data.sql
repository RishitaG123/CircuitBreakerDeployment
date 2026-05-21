-- Ensure table exists (safe if JPA creates it later)
CREATE TABLE IF NOT EXISTS authorized_user (
  id SERIAL PRIMARY KEY,
  user_id BIGINT UNIQUE NOT NULL
);

-- Insert seed users (idempotent)
INSERT INTO authorized_user (user_id) VALUES (1001) ON CONFLICT (user_id) DO NOTHING;
INSERT INTO authorized_user (user_id) VALUES (2002) ON CONFLICT (user_id) DO NOTHING;
