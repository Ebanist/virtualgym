CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'GYM_ADMIN', 'TRAINER')),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    family_id  UUID         NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    rotated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);

CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
