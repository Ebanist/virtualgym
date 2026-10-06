CREATE TABLE workout_plans (
    id                UUID PRIMARY KEY,
    owner_id          UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    gym_id            UUID          NOT NULL REFERENCES gyms (id),
    name              VARCHAR(100)  NOT NULL,
    description       VARCHAR(2000),
    archived          BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Przygotowanie pod wersję płatną: plany oficjalne siłowni od trenerów. W MVP tylko PRIVATE.
    visibility        VARCHAR(20)   NOT NULL DEFAULT 'PRIVATE' CHECK (visibility IN ('PRIVATE', 'GYM_OFFICIAL')),
    author_trainer_id UUID,
    deleted_at        TIMESTAMPTZ,
    version           BIGINT        NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_workout_plans_owner ON workout_plans (owner_id) WHERE deleted_at IS NULL;

CREATE TABLE plan_days (
    id         UUID PRIMARY KEY,
    plan_id    UUID         NOT NULL REFERENCES workout_plans (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    position   INT          NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_plan_days_plan ON plan_days (plan_id);

CREATE TABLE plan_items (
    id               UUID PRIMARY KEY,
    day_id           UUID          NOT NULL REFERENCES plan_days (id) ON DELETE CASCADE,
    exercise_id      UUID          NOT NULL REFERENCES exercises (id),
    equipment_id     UUID REFERENCES equipment (id),
    position         INT           NOT NULL,
    sets             INT           NOT NULL CHECK (sets BETWEEN 1 AND 20),
    reps_min         INT           NOT NULL CHECK (reps_min BETWEEN 1 AND 100),
    reps_max         INT           NOT NULL CHECK (reps_max BETWEEN 1 AND 100 AND reps_max >= reps_min),
    target_weight_kg NUMERIC(6, 2) CHECK (target_weight_kg IS NULL OR target_weight_kg >= 0),
    rest_seconds     INT           NOT NULL CHECK (rest_seconds BETWEEN 0 AND 900),
    note             VARCHAR(500),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_plan_items_day ON plan_items (day_id);
CREATE INDEX idx_plan_items_equipment ON plan_items (equipment_id);
