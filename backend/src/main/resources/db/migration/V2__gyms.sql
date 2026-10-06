CREATE TABLE gyms (
    id                         UUID PRIMARY KEY,
    name                       VARCHAR(120) NOT NULL,
    normalized_name            VARCHAR(120) NOT NULL,
    city                       VARCHAR(80)  NOT NULL,
    normalized_city            VARCHAR(80)  NOT NULL,
    address                    VARCHAR(200) NOT NULL,
    description                VARCHAR(2000),
    -- Przygotowanie pod wersję płatną: siłownia przejęta przez organizację i zweryfikowana.
    status                     VARCHAR(20)  NOT NULL DEFAULT 'COMMUNITY' CHECK (status IN ('COMMUNITY', 'VERIFIED')),
    claimed_by_organization_id UUID,
    created_by                 UUID         NOT NULL REFERENCES users (id),
    created_at                 TIMESTAMPTZ  NOT NULL,
    updated_at                 TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_gyms_normalized_city ON gyms (normalized_city);
CREATE INDEX idx_gyms_normalized_name_trgm ON gyms USING gin (normalized_name gin_trgm_ops);

CREATE TABLE gym_memberships (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    gym_id     UUID        NOT NULL REFERENCES gyms (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_gym_memberships_user_gym UNIQUE (user_id, gym_id)
);
CREATE INDEX idx_gym_memberships_gym ON gym_memberships (gym_id);
