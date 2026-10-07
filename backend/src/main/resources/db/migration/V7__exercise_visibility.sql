-- Widoczność własnych ćwiczeń: PRIVATE (tylko autor) / GYM (członkowie siłowni).
-- Dotychczasowe własne ćwiczenia były widoczne dla całej siłowni – zostają publiczne.
ALTER TABLE exercises
    ADD COLUMN visibility VARCHAR(10) NOT NULL DEFAULT 'GYM' CHECK (visibility IN ('PRIVATE', 'GYM')),
    -- Usunięcie przez autora ukrywa ćwiczenie z list; plany i historia treningów nadal je widzą.
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_exercises_created_by ON exercises (created_by);
