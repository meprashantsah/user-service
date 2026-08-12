CREATE TABLE IF NOT EXISTS user_profiles (
    id           UUID PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NOT NULL UNIQUE,
    avatar_url   VARCHAR(500),
    bio          VARCHAR(500),
    status       VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_display_name ON user_profiles (display_name);
CREATE INDEX IF NOT EXISTS idx_user_profiles_search
    ON user_profiles (username, display_name);