CREATE TABLE exercises (
    id              UUID PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    normalized_name VARCHAR(120)  NOT NULL,
    primary_muscle  VARCHAR(30)   NOT NULL,
    description     VARCHAR(2000),
    bodyweight      BOOLEAN       NOT NULL DEFAULT FALSE,
    -- GLOBAL: biblioteka (seed); CUSTOM: dodane przez użytkownika w konkretnej siłowni.
    scope           VARCHAR(10)   NOT NULL CHECK (scope IN ('GLOBAL', 'CUSTOM')),
    gym_id          UUID REFERENCES gyms (id) ON DELETE CASCADE,
    created_by      UUID REFERENCES users (id),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_exercises_custom_gym CHECK ((scope = 'CUSTOM') = (gym_id IS NOT NULL))
);
CREATE INDEX idx_exercises_gym ON exercises (gym_id);

CREATE TABLE exercise_secondary_muscles (
    exercise_id UUID        NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    muscle      VARCHAR(30) NOT NULL,
    PRIMARY KEY (exercise_id, muscle)
);

-- Typy sprzętu, na których można wykonać ćwiczenie (wystarczy jeden z nich).
CREATE TABLE exercise_equipment_types (
    exercise_id       UUID NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    equipment_type_id UUID NOT NULL REFERENCES equipment_types (id),
    PRIMARY KEY (exercise_id, equipment_type_id)
);

-- Jawne powiązanie konkretnego sprzętu w siłowni z ćwiczeniem (np. „Wyciąg górny #2” → „Ściąganie drążka”).
CREATE TABLE equipment_exercises (
    id           UUID PRIMARY KEY,
    equipment_id UUID        NOT NULL REFERENCES equipment (id) ON DELETE CASCADE,
    exercise_id  UUID        NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    created_by   UUID        NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_equipment_exercises UNIQUE (equipment_id, exercise_id)
);
CREATE INDEX idx_equipment_exercises_exercise ON equipment_exercises (exercise_id);

-- Seed globalnej biblioteki ćwiczeń.
-- secondary: mięśnie pomocnicze rozdzielone przecinkami; types: kody typów sprzętu (dowolny z nich wystarcza).
CREATE TEMPORARY TABLE seed_exercises (
    name        TEXT,
    primary_m   TEXT,
    secondary   TEXT,
    bodyweight  BOOLEAN,
    types       TEXT,
    description TEXT
) ON COMMIT DROP;

INSERT INTO seed_exercises VALUES
-- Klatka piersiowa
('Wyciskanie sztangi na ławce płaskiej', 'CHEST', 'TRICEPS,SHOULDERS', FALSE, 'BARBELL', 'Leżąc na ławce opuść sztangę do dolnej części klatki i wypchnij ją do wyprostu ramion.'),
('Wyciskanie sztangi na ławce skośnej', 'CHEST', 'SHOULDERS,TRICEPS', FALSE, 'BARBELL', 'Na ławce ustawionej pod kątem 30–45° wyciskaj sztangę znad górnej części klatki.'),
('Wyciskanie hantli na ławce płaskiej', 'CHEST', 'TRICEPS,SHOULDERS', FALSE, 'DUMBBELLS', 'Wyciskaj hantle znad klatki, prowadząc je lekko do środka w górnej fazie.'),
('Wyciskanie hantli na ławce skośnej', 'CHEST', 'SHOULDERS,TRICEPS', FALSE, 'DUMBBELLS', 'Wariant wyciskania hantli na ławce skośnej – akcent na górną część klatki.'),
('Rozpiętki z hantlami', 'CHEST', 'SHOULDERS', FALSE, 'DUMBBELLS', 'Leżąc na ławce, z lekko ugiętymi łokciami opuszczaj hantle łukiem na boki i zbliżaj nad klatką.'),
('Rozpiętki na maszynie (butterfly)', 'CHEST', 'SHOULDERS', FALSE, 'PEC_DECK', 'Siedząc na maszynie, zbliżaj ramiona przed sobą, kontrolując powrót.'),
('Krzyżowanie linek na bramie', 'CHEST', 'SHOULDERS', FALSE, 'CABLE_CROSSOVER', 'Stojąc między wyciągami, prowadź uchwyty łukiem w dół i do środka.'),
('Wyciskanie na maszynie siedząc', 'CHEST', 'TRICEPS,SHOULDERS', FALSE, 'CHEST_PRESS_MACHINE', 'Wypychaj uchwyty maszyny przed siebie do wyprostu ramion.'),
('Pompki', 'CHEST', 'TRICEPS,SHOULDERS,ABS', TRUE, '', 'W podporze przodem opuść klatkę do podłogi i wypchnij się, utrzymując proste ciało.'),
('Pompki na poręczach (dipy)', 'CHEST', 'TRICEPS,SHOULDERS', FALSE, 'DIP_STATION', 'Na poręczach opuszczaj ciało zginając łokcie, lekko pochylony do przodu, i wypchnij się w górę.'),
-- Plecy
('Podciąganie nachwytem', 'BACK', 'BICEPS,FOREARMS', FALSE, 'PULL_UP_BAR', 'Z zwisu na drążku podciągnij się, aż broda znajdzie się nad drążkiem.'),
('Podciąganie podchwytem', 'BACK', 'BICEPS', FALSE, 'PULL_UP_BAR', 'Podciąganie z dłońmi zwróconymi do siebie – większy udział bicepsów.'),
('Ściąganie drążka wyciągu górnego do klatki', 'BACK', 'BICEPS', FALSE, 'LAT_PULLDOWN', 'Siedząc, ściągaj drążek szerokim chwytem do górnej części klatki, ściągając łopatki.'),
('Ściąganie drążka wąskim chwytem', 'BACK', 'BICEPS', FALSE, 'LAT_PULLDOWN', 'Ściąganie wyciągu górnego wąskim uchwytem (V-bar) do klatki.'),
('Wiosłowanie na wyciągu dolnym', 'BACK', 'BICEPS,TRAPS', FALSE, 'SEATED_CABLE_ROW', 'Siedząc z prostymi plecami, przyciągaj uchwyt do brzucha.'),
('Wiosłowanie sztangą w opadzie tułowia', 'BACK', 'BICEPS,LOWER_BACK', FALSE, 'BARBELL', 'W opadzie tułowia z prostymi plecami przyciągaj sztangę do brzucha.'),
('Wiosłowanie hantlem jednorącz', 'BACK', 'BICEPS', FALSE, 'DUMBBELLS', 'Opierając kolano i dłoń o ławkę, przyciągaj hantel do biodra.'),
('Martwy ciąg', 'LOWER_BACK', 'GLUTES,HAMSTRINGS,TRAPS,FOREARMS', FALSE, 'BARBELL', 'Unieś sztangę z podłogi do pełnego wyprostu bioder, utrzymując neutralny kręgosłup.'),
('Prostowanie tułowia na ławce rzymskiej', 'LOWER_BACK', 'GLUTES,HAMSTRINGS', FALSE, 'ROMAN_CHAIR', 'Z opadu tułowia na ławce rzymskiej wróć do linii prostej ciała.'),
('Szrugsy', 'TRAPS', 'FOREARMS', FALSE, 'BARBELL,DUMBBELLS', 'Trzymając ciężar w wyprostowanych ramionach, unoś barki w kierunku uszu.'),
('Face pull na wyciągu', 'SHOULDERS', 'TRAPS,BACK', FALSE, 'CABLE_CROSSOVER', 'Przyciągaj linę z wyciągu ustawionego na wysokości twarzy, rozchylając dłonie na boki.'),
-- Barki
('Wyciskanie sztangi nad głowę', 'SHOULDERS', 'TRICEPS', FALSE, 'BARBELL', 'Stojąc, wypchnij sztangę z wysokości obojczyków nad głowę do wyprostu ramion.'),
('Wyciskanie hantli nad głowę siedząc', 'SHOULDERS', 'TRICEPS', FALSE, 'DUMBBELLS', 'Siedząc z oparciem, wyciskaj hantle znad barków nad głowę.'),
('Wznosy hantli bokiem', 'SHOULDERS', 'TRAPS', FALSE, 'DUMBBELLS', 'Unoś hantle bokiem do wysokości barków z lekko ugiętymi łokciami.'),
('Wznosy hantli w opadzie tułowia', 'SHOULDERS', 'BACK', FALSE, 'DUMBBELLS', 'W opadzie tułowia unoś hantle bokiem – akcent na tylny akton barków.'),
('Wyciskanie na maszynie nad głowę', 'SHOULDERS', 'TRICEPS', FALSE, 'SHOULDER_PRESS_MACHINE', 'Siedząc na maszynie wypychaj uchwyty nad głowę.'),
('Unoszenie ramienia bokiem na wyciągu', 'SHOULDERS', '', FALSE, 'CABLE_CROSSOVER', 'Stojąc bokiem do wyciągu dolnego, unoś uchwyt bokiem do wysokości barku.'),
-- Ramiona
('Uginanie ramion ze sztangą', 'BICEPS', 'FOREARMS', FALSE, 'BARBELL,EZ_BAR', 'Stojąc, uginaj ramiona ze sztangą, nie odchylając tułowia.'),
('Uginanie ramion z hantlami', 'BICEPS', 'FOREARMS', FALSE, 'DUMBBELLS', 'Uginaj ramiona z hantlami, obracając dłonie do supinacji w trakcie ruchu.'),
('Uginanie młotkowe', 'BICEPS', 'FOREARMS', FALSE, 'DUMBBELLS', 'Uginaj ramiona z hantlami trzymanymi chwytem neutralnym.'),
('Uginanie ramion na modlitewniku', 'BICEPS', '', FALSE, 'PREACHER_BENCH', 'Z ramionami opartymi o modlitewnik uginaj ramiona ze sztangą łamaną lub hantlem.'),
('Uginanie ramion na wyciągu dolnym', 'BICEPS', 'FOREARMS', FALSE, 'SEATED_CABLE_ROW,CABLE_CROSSOVER', 'Stojąc przodem do wyciągu dolnego, uginaj ramiona z drążkiem lub liną.'),
('Prostowanie ramion na wyciągu górnym', 'TRICEPS', '', FALSE, 'LAT_PULLDOWN,CABLE_CROSSOVER', 'Z łokciami przy tułowiu prostuj ramiona, ściągając drążek lub linę w dół.'),
('Wyciskanie francuskie', 'TRICEPS', '', FALSE, 'EZ_BAR,BARBELL', 'Leżąc na ławce, opuszczaj sztangę łamaną za głowę, zginając tylko łokcie.'),
('Prostowanie ramienia z hantlem nad głową', 'TRICEPS', '', FALSE, 'DUMBBELLS', 'Trzymając hantel nad głową, opuszczaj go za głowę i prostuj ramię.'),
('Pompki diamentowe', 'TRICEPS', 'CHEST', TRUE, '', 'Pompki z dłońmi złączonymi pod klatką – akcent na triceps.'),
-- Nogi i pośladki
('Przysiad ze sztangą', 'QUADRICEPS', 'GLUTES,HAMSTRINGS,LOWER_BACK', FALSE, 'SQUAT_RACK', 'Ze sztangą na barkach zejdź w przysiad co najmniej do równoległej ud i wstań.'),
('Przysiad przedni', 'QUADRICEPS', 'GLUTES,ABS', FALSE, 'SQUAT_RACK', 'Przysiad ze sztangą trzymaną z przodu na barkach – bardziej pionowy tułów.'),
('Przysiad na suwnicy Smitha', 'QUADRICEPS', 'GLUTES', FALSE, 'SMITH_MACHINE', 'Przysiad z gryfem prowadzonym w suwnicy Smitha.'),
('Przysiad bułgarski z hantlami', 'QUADRICEPS', 'GLUTES', FALSE, 'DUMBBELLS', 'Z tylną stopą opartą na ławce schodź w przysiad na przedniej nodze.'),
('Goblet squat', 'QUADRICEPS', 'GLUTES', FALSE, 'KETTLEBELL,DUMBBELLS', 'Przysiad z kettlebellem lub hantlem trzymanym przy klatce.'),
('Wypychanie na suwnicy (leg press)', 'QUADRICEPS', 'GLUTES,HAMSTRINGS', FALSE, 'LEG_PRESS', 'Wypychaj platformę nogami, nie odrywając lędźwi od oparcia.'),
('Hack przysiad', 'QUADRICEPS', 'GLUTES', FALSE, 'HACK_SQUAT', 'Przysiad na maszynie hack z plecami opartymi o oparcie.'),
('Prostowanie nóg na maszynie', 'QUADRICEPS', '', FALSE, 'LEG_EXTENSION', 'Siedząc, prostuj nogi w kolanach przeciw oporowi wałka.'),
('Uginanie nóg na maszynie', 'HAMSTRINGS', 'CALVES', FALSE, 'LEG_CURL', 'Uginaj nogi w kolanach, przyciągając wałek do pośladków.'),
('Rumuński martwy ciąg', 'HAMSTRINGS', 'GLUTES,LOWER_BACK', FALSE, 'BARBELL,DUMBBELLS', 'Z lekko ugiętymi kolanami prowadź ciężar wzdłuż nóg, cofając biodra.'),
('Hip thrust', 'GLUTES', 'HAMSTRINGS', FALSE, 'BARBELL,HIP_THRUST_MACHINE', 'Oparty łopatkami o ławkę, wypychaj biodra w górę z obciążeniem na biodrach.'),
('Wspięcia na palce na maszynie', 'CALVES', '', FALSE, 'CALF_RAISE_MACHINE', 'Unoś pięty, wspinając się na palce w pełnym zakresie ruchu.'),
('Odwodzenie nóg na maszynie', 'ABDUCTORS', 'GLUTES', FALSE, 'HIP_ABDUCTION_MACHINE', 'Siedząc, odwodź nogi na zewnątrz przeciw oporowi maszyny.'),
('Swing kettlebell', 'GLUTES', 'HAMSTRINGS,LOWER_BACK,SHOULDERS', FALSE, 'KETTLEBELL', 'Dynamicznym wyprostem bioder wymachuj kettlebell do wysokości klatki.'),
('Przysiad z masą ciała', 'QUADRICEPS', 'GLUTES', TRUE, '', 'Przysiad bez obciążenia z ramionami wyciągniętymi przed siebie.'),
('Wykroki', 'QUADRICEPS', 'GLUTES,HAMSTRINGS', TRUE, '', 'Zrób krok w przód i obniż biodra, aż oba kolana będą zgięte pod kątem prostym.'),
('Mostek biodrowy', 'GLUTES', 'HAMSTRINGS', TRUE, '', 'Leżąc na plecach ze zgiętymi nogami, unoś biodra, napinając pośladki.'),
('Wskoki na skrzynię', 'QUADRICEPS', 'GLUTES,CALVES', FALSE, 'PLYO_BOX', 'Z obunóż wskocz na skrzynię, lądując miękko, i zejdź.'),
-- Brzuch
('Deska (plank)', 'ABS', 'SHOULDERS,LOWER_BACK', TRUE, '', 'Utrzymuj podpór na przedramionach z ciałem w jednej linii.'),
('Brzuszki', 'ABS', '', TRUE, '', 'Leżąc na plecach, unoś łopatki nad podłogę, napinając brzuch.'),
('Unoszenie nóg leżąc', 'ABS', '', TRUE, '', 'Leżąc na plecach, unoś proste nogi do pionu i powoli opuszczaj.'),
('Unoszenie nóg w zwisie', 'ABS', 'FOREARMS', FALSE, 'PULL_UP_BAR', 'W zwisie na drążku unoś kolana lub proste nogi do klatki.'),
('Rollout na kółku', 'ABS', 'SHOULDERS,LOWER_BACK', FALSE, 'AB_WHEEL', 'Z klęku tocz kółko przed siebie, utrzymując napięty brzuch, i wróć.'),
('Spięcia brzucha na wyciągu (allahy)', 'ABS', '', FALSE, 'LAT_PULLDOWN,CABLE_CROSSOVER', 'Klęcząc przy wyciągu górnym, zginaj tułów, przyciągając linę w dół.'),
('Rosyjskie skręty', 'OBLIQUES', 'ABS', TRUE, '', 'Siedząc z uniesionymi stopami, skręcaj tułów na boki.'),
('Wspinaczka (mountain climbers)', 'ABS', 'SHOULDERS,QUADRICEPS', TRUE, '', 'W podporze przodem dynamicznie przyciągaj kolana do klatki naprzemiennie.'),
-- Cardio / całe ciało
('Bieg na bieżni', 'CARDIO', 'QUADRICEPS,CALVES', FALSE, 'TREADMILL', 'Bieg lub marsz na bieżni w zadanym tempie i nachyleniu.'),
('Jazda na rowerze stacjonarnym', 'CARDIO', 'QUADRICEPS', FALSE, 'STATIONARY_BIKE', 'Jazda na rowerze stacjonarnym w stałym tempie lub interwałach.'),
('Wiosłowanie na ergometrze', 'CARDIO', 'BACK,QUADRICEPS', FALSE, 'ROWING_MACHINE', 'Odpychaj się nogami, następnie przyciągaj uchwyt do brzucha.'),
('Trening na orbitreku', 'CARDIO', '', FALSE, 'ELLIPTICAL', 'Ruch eliptyczny nogami i rękami w stałym tempie.'),
('Burpees', 'FULL_BODY', 'CHEST,QUADRICEPS', TRUE, '', 'Przysiad, wyrzut nóg do podporu, pompka, powrót i wyskok.');

INSERT INTO exercises (id, name, normalized_name, primary_muscle, description, bodyweight, scope)
SELECT gen_random_uuid(), s.name,
       -- Odpowiednik TextNormalizer.normalize dla polskich znaków.
       trim(regexp_replace(translate(lower(s.name), 'ąćęłńóśźż', 'acelnoszz'), '[^a-z0-9]+', ' ', 'g')),
       s.primary_m, s.description, s.bodyweight, 'GLOBAL'
FROM seed_exercises s;

INSERT INTO exercise_secondary_muscles (exercise_id, muscle)
SELECT e.id, m.muscle
FROM seed_exercises s
JOIN exercises e ON e.name = s.name AND e.scope = 'GLOBAL'
CROSS JOIN LATERAL unnest(string_to_array(s.secondary, ',')) AS m(muscle)
WHERE m.muscle <> '';

INSERT INTO exercise_equipment_types (exercise_id, equipment_type_id)
SELECT e.id, t.id
FROM seed_exercises s
JOIN exercises e ON e.name = s.name AND e.scope = 'GLOBAL'
CROSS JOIN LATERAL unnest(string_to_array(s.types, ',')) AS c(code)
JOIN equipment_types t ON t.code = c.code
WHERE c.code <> '';
