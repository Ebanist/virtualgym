CREATE TABLE workout_sessions (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    gym_id      UUID         NOT NULL REFERENCES gyms (id),
    plan_id     UUID REFERENCES workout_plans (id) ON DELETE SET NULL,
    plan_day_id UUID REFERENCES plan_days (id) ON DELETE SET NULL,
    -- Migawka nazwy (plan/dzień mogą się później zmienić lub zniknąć).
    title       VARCHAR(220) NOT NULL,
    status      VARCHAR(20)  NOT NULL CHECK (status IN ('IN_PROGRESS', 'FINISHED', 'ABANDONED')),
    started_at  TIMESTAMPTZ  NOT NULL,
    finished_at TIMESTAMPTZ,
    note        VARCHAR(1000),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
-- Co najwyżej jeden trwający trening na użytkownika.
CREATE UNIQUE INDEX uq_workout_sessions_active ON workout_sessions (user_id) WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_workout_sessions_user_finished ON workout_sessions (user_id, finished_at DESC);

CREATE TABLE session_exercises (
    id               UUID PRIMARY KEY,
    session_id       UUID         NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
    exercise_id      UUID         NOT NULL REFERENCES exercises (id),
    equipment_id     UUID REFERENCES equipment (id),
    position         INT          NOT NULL,
    -- Cel z planu (migawka); null przy treningu ad hoc.
    target_reps_min  INT,
    target_reps_max  INT,
    target_weight_kg NUMERIC(6, 2),
    rest_seconds     INT          NOT NULL DEFAULT 90,
    note             VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_session_exercises_session ON session_exercises (session_id);
CREATE INDEX idx_session_exercises_exercise ON session_exercises (exercise_id);

CREATE TABLE session_sets (
    id                  UUID PRIMARY KEY,
    session_exercise_id UUID          NOT NULL REFERENCES session_exercises (id) ON DELETE CASCADE,
    set_number          INT           NOT NULL,
    reps                INT CHECK (reps IS NULL OR reps BETWEEN 0 AND 1000),
    weight_kg           NUMERIC(6, 2) CHECK (weight_kg IS NULL OR weight_kg >= 0),
    completed           BOOLEAN       NOT NULL DEFAULT FALSE,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_session_sets_exercise ON session_sets (session_exercise_id);
