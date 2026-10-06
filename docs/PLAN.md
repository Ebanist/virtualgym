# GymPlanner – MVP (wersja free) – plan

## Kontekst
Repozytorium `ebanist/virtualgym` jest puste (branch `claude/gymplanner-mvp-free-lbmx6x`, brak commitów). Budujemy od zera monorepo: backend Spring Boot 3 (Java 21) + frontend React 18/TS/Vite, Postgres 16 w docker-compose. Cel: działające MVP wersji free (konta, siłownie, sprzęt społecznościowy, ćwiczenia, planer, tryb treningu), z modelem danych gotowym na wersję płatną. Praca w 6 etapach, po każdym: build + testy zielone, commit + push, podsumowanie.

Środowisko: Java 21, Node 22, Docker dostępne (Testcontainers zadziała).

---

## Decyzje uzgodnione z użytkownikiem
- Ćwiczenie ↔ sprzęt: globalny słownik typów sprzętu (dopasowanie automatyczne) **+** ręczne powiązania z konkretnym sprzętem.
- Własne ćwiczenia: widoczne dla **wszystkich członków siłowni**, w której zostały dodane.
- Status „usunięty z siłowni”: ustawia **ręcznie dowolny członek** (zapis w historii, można cofnąć); zgłoszenia są informacyjne, zgłoszenie typu REMOVED podpowiada tę akcję.
- Tryb treningu: z dnia planu **oraz ad hoc** (pusty trening, ćwiczenia dodawane z listy dostępnych w siłowni).

## 1. Model danych (encje i relacje)

Wspólne: `id` (UUID), `createdAt`, `updatedAt` (auditing JPA), soft delete = `deletedAt` (nullable) + `@SQLRestriction("deleted_at is null")` tam, gdzie wymagane.

**Konta**
- `User`: email (unique, lower-case), passwordHash (BCrypt), displayName, role (`USER` | `GYM_ADMIN` | `TRAINER`; w MVP tylko USER), createdAt
- `RefreshToken`: user, tokenHash (SHA-256), expiresAt, revokedAt, replacedBy (rotacja; wykrycie reuse → unieważnienie rodziny)
- `PasswordResetToken`: user, tokenHash, expiresAt, usedAt

**Siłownie**
- `Gym`: name, normalizedName (lower, bez polskich znaków – do wyszukiwania/duplikatów), city, normalizedCity, address, description?, createdBy(User), **status** (`COMMUNITY` | `VERIFIED`, default COMMUNITY), **claimedByOrganizationId** (UUID, nullable, bez FK – organizacje powstaną później)
- `GymMembership`: user, gym, joinedAt; unique(user, gym)
- Liczba członków liczona zapytaniem (`count`) – bez denormalizacji w MVP.

**Sprzęt**
- `EquipmentType` (globalny słownik, seed): code, name (np. „Wyciąg górny”, „Sztanga”, „Hantle”, „Ławka płaska”, „Suwnica”), category
- `Equipment`: gym, name, normalizedName, category (enum: `STRENGTH_MACHINE`, `CABLE`, `FREE_WEIGHTS`, `BENCH`, `CARDIO`, `FUNCTIONAL`, `OTHER`), equipmentType? (FK → słownik), description?, quantity?, photo? (FK → `StoredFile`), status (`ACTIVE` | `REMOVED_FROM_GYM`), **source** (`COMMUNITY` | `GYM_OFFICIAL`), **verified** (bool, false), createdBy, deletedAt (soft delete), `@Version`
- `EquipmentChange` (historia): equipment, user, changedAt, changeType (`CREATED`, `UPDATED`, `PHOTO_ADDED`, `STATUS_CHANGED`, `DELETED`), changes (JSONB: `{field: {old, new}}`)
- `EquipmentReport`: equipment, reporter, type (`DUPLICATE`, `WRONG_DATA`, `REMOVED_FROM_GYM`), comment?, duplicateOf? (Equipment), status (`OPEN` | `RESOLVED`), createdAt
- `StoredFile`: storageKey, thumbnailKey, contentType, sizeBytes, uploadedBy

**Ćwiczenia**
- `Exercise`: name, normalizedName, primaryMuscle (enum `MuscleGroup`), description, **bodyweight** (bool), scope (`GLOBAL` | `CUSTOM`), createdBy? (dla CUSTOM), gym? (dla CUSTOM)
- `exercise_secondary_muscles` (ElementCollection enum)
- `exercise_equipment_types` (M:N Exercise ↔ EquipmentType – „wymaga jednego z typów”)
- `EquipmentExercise` (M:N Equipment ↔ Exercise – jawne powiązanie w danej siłowni, np. „Wyciąg górny #2” → „Ściąganie drążka”; tworzone automatycznie dla ćwiczeń CUSTOM i ręcznie przez członków)

**Reguła dostępności ćwiczenia w siłowni G** (logika w czystej klasie `ExerciseAvailabilityResolver` – testy jednostkowe):
ćwiczenie dostępne ⇔ `bodyweight` **lub** istnieje aktywny (nieusunięty, status ACTIVE) sprzęt w G, którego `equipmentType` ∈ typy ćwiczenia **lub** istnieje jawne `EquipmentExercise` z aktywnym sprzętem w G. Wynik zawiera listę pasującego sprzętu (do wyboru w pozycji planu).

**Planer**
- `WorkoutPlan`: owner(User), gym, name, description?, archived (bool), **visibility** (`PRIVATE` | `GYM_OFFICIAL`, MVP: PRIVATE), **authorTrainerId** (UUID nullable), deletedAt, `@Version`
- `PlanDay`: plan, name („Dzień A – push”), position
- `PlanItem`: day, exercise, equipment? (null dla masy ciała), position, sets, repsMin, repsMax, targetWeightKg?, restSeconds, note?
- Ostrzeżenie „sprzęt usunięty” liczone w DTO (`equipmentUnavailable = equipment.status==REMOVED || deleted`) – bez przechowywania flagi.

**Trening / dziennik**
- `WorkoutSession`: user, gym, plan?, planDay?, startedAt, finishedAt?, status (`IN_PROGRESS` | `FINISHED` | `ABANDONED`), note?
- `SessionExercise`: session, exercise, equipment?, position, planItem? (snapshot docelowych wartości: sets/reps/weight/rest)
- `SessionSet`: sessionExercise, setNumber, reps?, weightKg?, completed (bool), completedAt
- „Poprzedni wynik” = ostatni FINISHED `SessionExercise` usera dla danego ćwiczenia (zapytanie).

---

## 2. Endpointy REST (`/api/v1`, JSON, błędy jako `ProblemDetail` RFC 7807, paginacja `?page&size&sort` → `PageResponse{content,page,size,totalElements,totalPages}`)

**Auth** (`/auth`, publiczne)
- `POST /auth/register`, `POST /auth/login` → `{accessToken, user}` + refresh token w ciasteczku httpOnly (`SameSite=Strict`, path `/api/v1/auth`)
- `POST /auth/refresh` (rotacja), `POST /auth/logout`
- `POST /auth/password-reset/request` (link logowany w konsoli), `POST /auth/password-reset/confirm`

**Profil** – `GET /me`, `PATCH /me`, `POST /me/password`

**Siłownie**
- `GET /gyms?q=&city=` (paginacja), `POST /gyms` (gdy wykryto podobne → `409` z listą kandydatów, ponowne wysłanie z `confirmDuplicate=true` zapisuje), `GET /gyms/similar?name=&city=` (podpowiedź na żywo w formularzu)
- `GET /gyms/{id}` (dane + memberCount + isMember), `POST /gyms/{id}/membership`, `DELETE /gyms/{id}/membership`, `GET /me/gyms`

**Sprzęt**
- `GET /gyms/{gymId}/equipment?q=&category=&status=` (paginacja)
- `GET /gyms/{gymId}/equipment/similar?name=`
- `POST /gyms/{gymId}/equipment` (członek), `GET /equipment/{id}`, `PATCH /equipment/{id}` (członek, z `version`), `DELETE /equipment/{id}` (członek, soft)
- `POST /equipment/{id}/photo` (multipart, członek), `GET /files/{id}` i `GET /files/{id}/thumbnail`
- `GET /equipment/{id}/history` (paginacja)
- `GET /equipment/{id}/reports`, `POST /equipment/{id}/reports`, `PATCH /reports/{id}` (resolve – członek)
- `GET /equipment-types`

**Ćwiczenia**
- `GET /exercises?q=&muscle=` (globalne + własne usera)
- `GET /gyms/{gymId}/exercises/available?q=&muscle=` (z listą pasującego sprzętu)
- `POST /gyms/{gymId}/exercises` (własne ćwiczenie, członek, `equipmentIds[]`)
- `POST /equipment/{id}/exercises/{exerciseId}`, `DELETE …` (powiązanie, członek)
- `GET /equipment/{id}/exercises`

**Plany** (tylko właściciel; inaczej `404`)
- `GET /plans?gymId=&archived=`, `POST /plans`, `GET /plans/{id}`, `PATCH /plans/{id}`, `DELETE /plans/{id}` (soft)
- `POST /plans/{id}/copy`, `POST /plans/{id}/archive`, `POST /plans/{id}/unarchive`
- `POST /plans/{id}/days`, `PATCH /days/{id}`, `DELETE /days/{id}`, `PUT /plans/{id}/days/order`
- `POST /days/{id}/items` (walidacja: ćwiczenie dostępne w siłowni planu, sprzęt należy do tej siłowni i pasuje), `PATCH /items/{id}`, `DELETE /items/{id}`, `PUT /days/{id}/items/order` (`[itemId…]`) + `POST /items/{id}/move?direction=UP|DOWN`

**Trening**
- `POST /sessions` (`{planDayId}` → kopia pozycji jako SessionExercise/Sets; albo `{gymId}` → pusty trening ad hoc), `GET /sessions/active`
- `POST /sessions/{id}/exercises` (`{exerciseId, equipmentId?}` – walidacja dostępności w siłowni; używane w ad hoc i do dodania ćwiczenia w treningu z planu), `DELETE /sessions/{id}/exercises/{seId}`
- `GET /sessions/{id}` (z `previousResult` per ćwiczenie), `PATCH /sessions/{id}/sets/{setId}` (reps, weight, completed), `POST /sessions/{id}/exercises/{seId}/sets` (dodatkowa seria)
- `POST /sessions/{id}/finish`, `POST /sessions/{id}/abandon`
- `GET /sessions?page=` (historia), `GET /exercises/{id}/history` (ostatnie wyniki)

Swagger UI: `/swagger-ui.html`, spec: `/v3/api-docs`.

---

## 3. Struktura katalogów

```
/backend   (Maven, pakiet com.gymplanner)
  src/main/java/com/gymplanner/
    config/           (Security, OpenAPI, Jackson, Clock, Storage props)
    common/           (error/ProblemDetail handler, PageResponse, text/Normalizer, BaseEntity)
    auth/             (controller, service, jwt/, dto/, entity, repo)
    user/  gym/  equipment/  storage/  exercise/  plan/  workout/
      └ każdy moduł: XController, XService, XRepository, entity/, dto/, XMapper (ręczne mapowanie)
    dev/              (DevDataSeeder – profil `dev`)
  src/main/resources/db/migration  (V1__auth.sql, V2__gyms.sql, … R__ seed ćwiczeń jako V-migracja)
  src/test/…        (AbstractIntegrationTest z Testcontainers + @ServiceConnection)

/frontend
  src/
    api/              (client.ts – fetch + refresh na 401; schema.d.ts – generowane; per-moduł hooki TanStack Query: gyms.ts, equipment.ts, plans.ts, …)
    app/              (router.tsx, providers.tsx, AuthContext, RequireAuth)
    i18n/             (index.ts, locales/pl/*.json)
    components/       (Button, Field, Modal, Pagination, EmptyState, ErrorMessage, PhotoUpload, RestTimer, ReorderList)
    features/
      auth/  profile/  gyms/  equipment/  exercises/  plans/  workout/  history/
        └ pages (XPage.tsx), formularze (RHF + Zod schema.ts), *.module.css, *.test.tsx
    styles/           (tokens.css – zmienne CSS, reset, global)
  vite.config.ts (proxy /api → :8080), vitest setup
docker-compose.yml   (postgres:16, volume, healthcheck)
README.md, CLAUDE.md
```

**Klient API – decyzja:** typy generowane z OpenAPI (`openapi-typescript` → `src/api/schema.d.ts`, skrypt `npm run gen:api` czytający `/v3/api-docs` lub zapisany `openapi.json` w repo) + cienki ręczny wrapper `openapi-fetch`/własny `fetch` z obsługą tokenu i refresh. Uzasadnienie: jedno źródło prawdy dla kontraktu (backend), brak dryfu typów, zero ciężkiego generowanego kodu (tylko typy), a hooki TanStack Query piszemy ręcznie – czytelne i łatwe do zmiany. Wygenerowany `openapi.json` commitujemy, żeby frontend budował się bez działającego backendu.

**Tokeny:** access token (15 min) w pamięci (React context), refresh token (30 dni) w ciasteczku httpOnly z rotacją – odporne na XSS, odświeżenie strony = cichy `POST /auth/refresh`.

**Zdjęcia:** `FileStorage` (`store`, `load`, `delete`) + `LocalFileStorage` (katalog z `app.storage.local-dir`), limit 5 MB (konfigurowalny), walidacja typu po magic bytes (jpg/png/webp), miniatury 320px. Generowanie: Thumbnailator + TwelveMonkeys `imageio-webp` (odczyt WebP); miniatura zapisywana jako JPEG.

---

## 4. Etapy

1. **Szkielet + auth** – monorepo, docker-compose, Spring Boot (security JWT, ProblemDetail, OpenAPI, Flyway V1), `User`/role/refresh/reset; frontend: Vite, router, i18n, TanStack Query, klient API, ekrany login/rejestracja/reset/profil/zmiana hasła; testy integracyjne auth, Vitest dla formularza logowania; README + CLAUDE.md.
2. **Siłownie i członkostwo** – wyszukiwanie, dodawanie z ostrzeżeniem o duplikacie (normalizacja + `pg_trgm similarity` w tym samym mieście), join/leave, „Moje siłownie”, profil siłowni; seed dev (2 siłownie, 2 userów: `anna@example.com`, `jan@example.com` / `Password123!`).
3. **Sprzęt** – CRUD z autoryzacją członkostwa, podpowiedzi podobnych nazw, filtr/wyszukiwanie, zdjęcia + miniatury, historia zmian (JSONB diff), zgłoszenia, status REMOVED, soft delete; seed ~20 sztuk.
4. **Ćwiczenia** – słownik `EquipmentType`, seed ~50 ćwiczeń (PL), powiązania sprzęt↔ćwiczenie, własne ćwiczenia, `ExerciseAvailabilityResolver` + testy jednostkowe, endpoint dostępnych ćwiczeń.
5. **Planer** – plany/dni/pozycje, walidacja dostępności, kolejność (przyciski ↑/↓ na mobile + `PUT order`), kopiowanie/archiwizacja/usuwanie, ostrzeżenie o usuniętym sprzęcie; seed przykładowych planów; testy integracyjne planów (w tym dostęp tylko dla właściciela).
6. **Trening i historia** – start z dnia planu lub ad hoc (dodawanie ćwiczeń z listy dostępnych), widok mobilny (ćwiczenie, zdjęcie sprzętu, serie do odhaczenia, reps/ciężar, timer przerwy działający na `Date.now()` odpornie na uśpienie karty), poprzedni wynik, zakończenie/porzucenie, historia z paginacją.

Po każdym etapie: `./mvnw verify` + `npm run lint && npm test && npm run build` zielone, commit + push na `claude/gymplanner-mvp-free-lbmx6x`, podsumowanie (zmiany / jak testować ręcznie / co zostało), aktualizacja README i CLAUDE.md.

---

## 5. Weryfikacja
- Backend: `cd backend && ./mvnw verify` – testy integracyjne (Testcontainers Postgres): auth (rejestracja, login, refresh z rotacją, 401), sprzęt (członek vs nieczłonek → 403, historia, zgłoszenia, upload zdjęcia), plany (dodanie ćwiczenia niedostępnego → 422, cudzy plan → 404, kopiowanie, ostrzeżenie po oznaczeniu sprzętu jako usuniętego); jednostkowe `ExerciseAvailabilityResolverTest`.
- Frontend: `cd frontend && npm run lint && npm test && npm run build` (Vitest + RTL: formularz logowania, formularz sprzętu z podpowiedziami, lista pozycji planu z przesuwaniem ↑/↓, licznik/serie w trybie treningu).
- E2E ręcznie: `docker compose up -d`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`, `npm run dev`, login jako seedowany user → siłownia → sprzęt → plan → trening → historia; Swagger UI pod `/swagger-ui.html`. Dodatkowo szybki smoke test w Chromium (Playwright) widoków mobilnych 375px.
