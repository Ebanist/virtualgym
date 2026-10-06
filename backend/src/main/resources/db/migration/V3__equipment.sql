CREATE TABLE stored_files (
    id            UUID PRIMARY KEY,
    storage_key   VARCHAR(200) NOT NULL UNIQUE,
    thumbnail_key VARCHAR(200),
    content_type  VARCHAR(50)  NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    width         INT          NOT NULL,
    height        INT          NOT NULL,
    uploaded_by   UUID         NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

-- Globalny słownik typów sprzętu. Ćwiczenia wymagają typów; sprzęt w siłowni może wskazywać typ.
CREATE TABLE equipment_types (
    id         UUID PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    category   VARCHAR(30)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO equipment_types (id, code, name, category) VALUES
    (gen_random_uuid(), 'BARBELL', 'Sztanga', 'FREE_WEIGHTS'),
    (gen_random_uuid(), 'EZ_BAR', 'Gryf łamany', 'FREE_WEIGHTS'),
    (gen_random_uuid(), 'DUMBBELLS', 'Hantle', 'FREE_WEIGHTS'),
    (gen_random_uuid(), 'KETTLEBELL', 'Kettlebell', 'FREE_WEIGHTS'),
    (gen_random_uuid(), 'SQUAT_RACK', 'Klatka / stojak do przysiadów', 'FREE_WEIGHTS'),
    (gen_random_uuid(), 'LANDMINE', 'Landmine', 'FREE_WEIGHTS'),
    (gen_random_uuid(), 'FLAT_BENCH', 'Ławka płaska', 'BENCH'),
    (gen_random_uuid(), 'ADJUSTABLE_BENCH', 'Ławka regulowana', 'BENCH'),
    (gen_random_uuid(), 'DECLINE_BENCH', 'Ławka skośna ujemna', 'BENCH'),
    (gen_random_uuid(), 'PREACHER_BENCH', 'Modlitewnik', 'BENCH'),
    (gen_random_uuid(), 'ROMAN_CHAIR', 'Ławka rzymska', 'BENCH'),
    (gen_random_uuid(), 'LAT_PULLDOWN', 'Wyciąg górny', 'CABLE'),
    (gen_random_uuid(), 'SEATED_CABLE_ROW', 'Wyciąg dolny (wiosłowanie)', 'CABLE'),
    (gen_random_uuid(), 'CABLE_CROSSOVER', 'Brama / wyciąg krzyżowy', 'CABLE'),
    (gen_random_uuid(), 'SMITH_MACHINE', 'Suwnica Smitha', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'LEG_PRESS', 'Suwnica do nóg (leg press)', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'HACK_SQUAT', 'Hack przysiad', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'LEG_EXTENSION', 'Maszyna do prostowania nóg', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'LEG_CURL', 'Maszyna do uginania nóg', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'CHEST_PRESS_MACHINE', 'Maszyna do wyciskania (klatka)', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'PEC_DECK', 'Butterfly (pec deck)', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'SHOULDER_PRESS_MACHINE', 'Maszyna do wyciskania nad głowę', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'CALF_RAISE_MACHINE', 'Maszyna do łydek', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'HIP_THRUST_MACHINE', 'Maszyna do hip thrustów', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'HIP_ABDUCTION_MACHINE', 'Maszyna do odwodzenia / przywodzenia', 'STRENGTH_MACHINE'),
    (gen_random_uuid(), 'TREADMILL', 'Bieżnia', 'CARDIO'),
    (gen_random_uuid(), 'STATIONARY_BIKE', 'Rower stacjonarny', 'CARDIO'),
    (gen_random_uuid(), 'ROWING_MACHINE', 'Ergometr wioślarski', 'CARDIO'),
    (gen_random_uuid(), 'ELLIPTICAL', 'Orbitrek', 'CARDIO'),
    (gen_random_uuid(), 'STAIR_CLIMBER', 'Schody / stepper', 'CARDIO'),
    (gen_random_uuid(), 'PULL_UP_BAR', 'Drążek do podciągania', 'FUNCTIONAL'),
    (gen_random_uuid(), 'DIP_STATION', 'Poręcze do dipów', 'FUNCTIONAL'),
    (gen_random_uuid(), 'RESISTANCE_BAND', 'Gumy oporowe', 'FUNCTIONAL'),
    (gen_random_uuid(), 'SUSPENSION_TRAINER', 'Taśmy TRX', 'FUNCTIONAL'),
    (gen_random_uuid(), 'MEDICINE_BALL', 'Piłka lekarska', 'FUNCTIONAL'),
    (gen_random_uuid(), 'PLYO_BOX', 'Skrzynia plyometryczna', 'FUNCTIONAL'),
    (gen_random_uuid(), 'AB_WHEEL', 'Kółko do brzucha', 'FUNCTIONAL');

CREATE TABLE equipment (
    id                UUID PRIMARY KEY,
    gym_id            UUID          NOT NULL REFERENCES gyms (id) ON DELETE CASCADE,
    name              VARCHAR(120)  NOT NULL,
    normalized_name   VARCHAR(120)  NOT NULL,
    category          VARCHAR(30)   NOT NULL CHECK (category IN
                          ('STRENGTH_MACHINE', 'CABLE', 'FREE_WEIGHTS', 'BENCH', 'CARDIO', 'FUNCTIONAL', 'OTHER')),
    equipment_type_id UUID REFERENCES equipment_types (id),
    description       VARCHAR(2000),
    quantity          INT CHECK (quantity IS NULL OR quantity > 0),
    photo_id          UUID REFERENCES stored_files (id),
    status            VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REMOVED_FROM_GYM')),
    -- Przygotowanie pod wersję płatną: sprzęt oficjalny siłowni i weryfikacja.
    source            VARCHAR(20)   NOT NULL DEFAULT 'COMMUNITY' CHECK (source IN ('COMMUNITY', 'GYM_OFFICIAL')),
    verified          BOOLEAN       NOT NULL DEFAULT FALSE,
    created_by        UUID          NOT NULL REFERENCES users (id),
    deleted_at        TIMESTAMPTZ,
    version           BIGINT        NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_equipment_gym ON equipment (gym_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_equipment_normalized_name_trgm ON equipment USING gin (normalized_name gin_trgm_ops);

CREATE TABLE equipment_changes (
    id           UUID PRIMARY KEY,
    equipment_id UUID        NOT NULL REFERENCES equipment (id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users (id),
    change_type  VARCHAR(30) NOT NULL,
    changes      JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_equipment_changes_equipment ON equipment_changes (equipment_id, created_at DESC);

CREATE TABLE equipment_reports (
    id              UUID PRIMARY KEY,
    equipment_id    UUID          NOT NULL REFERENCES equipment (id) ON DELETE CASCADE,
    reporter_id     UUID          NOT NULL REFERENCES users (id),
    type            VARCHAR(30)   NOT NULL CHECK (type IN ('DUPLICATE', 'WRONG_DATA', 'REMOVED_FROM_GYM')),
    comment         VARCHAR(1000),
    duplicate_of_id UUID REFERENCES equipment (id),
    status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    resolved_by     UUID REFERENCES users (id),
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_equipment_reports_equipment ON equipment_reports (equipment_id);
