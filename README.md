# GymPlanner

Aplikacja dla bywalców siłowni: wybierasz swoją siłownię, widzisz sprzęt dodany przez jej społeczność
i układasz plany treningowe wyłącznie na tym sprzęcie. Repozytorium zawiera MVP wersji **free** (web, RWD).

Plan MVP (model danych, endpointy, etapy): [`docs/PLAN.md`](docs/PLAN.md).

## Status

| Etap | Zakres | Stan |
|------|--------|------|
| 1 | Szkielet monorepo, docker-compose, konfiguracja, auth | ✅ |
| 2 | Siłownie i członkostwo | ✅ |
| 3 | Sprzęt (zdjęcia, historia zmian, zgłoszenia) | ✅ |
| 4 | Ćwiczenia i powiązania ze sprzętem | ✅ |
| 5 | Planer treningowy | ✅ |
| 6 | Tryb treningu i historia | ⏳ |

## Wymagania

- Java 21 (Maven jest w repo jako wrapper `./mvnw`)
- Node.js 20+ (testowane na 22)
- Docker (PostgreSQL lokalnie oraz Testcontainers w testach backendu)

## Uruchomienie lokalne

```bash
# 1. Baza danych (PostgreSQL 16 na localhost:5432, user/hasło/baza: gymplanner)
docker compose up -d

# 2. Backend (http://localhost:8080, Swagger UI: http://localhost:8080/swagger-ui.html)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # profil dev = dane testowe

# 3. Frontend (http://localhost:5173, proxy /api -> :8080)
cd frontend
npm install
npm run dev
```

Otwórz http://localhost:5173 i zaloguj się kontem testowym albo załóż nowe.

**Dane testowe (profil `dev`, tylko na pustej bazie):**

| E-mail | Hasło | Siłownie |
|--------|-------|----------|
| `anna@example.com` | `Password123` | Fitness Arena Centrum (Warszawa) |
| `jan@example.com` | `Password123` | Fitness Arena Centrum, Iron Gym Kazimierz (Kraków) |

Seed zawiera też przykładowe plany („FBW – 2 dni” Anny – z pozycją na usuniętej suwnicy Smitha, która pokazuje
ostrzeżenie – oraz „Trójbój – podstawa” Jana) i 21 sztuk sprzętu (15 w Fitness Arena – w tym „Suwnica Smitha” oznaczona jako usunięta z siłowni, 6 w Iron Gym).

Reset bazy: `docker compose down -v && docker compose up -d`.

**Reset hasła:** w MVP nie wysyłamy e-maili – link do resetu pojawia się w logach backendu
(`Password reset link for ...`).

### Konfiguracja backendu (zmienne środowiskowe)

| Zmienna | Domyślnie | Opis |
|---------|-----------|------|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/gymplanner` / `gymplanner` / `gymplanner` | Połączenie z bazą |
| `JWT_SECRET` | wartość deweloperska | Sekret HMAC dla JWT (min. 32 bajty) – **zmień poza dev** |
| `SECURE_COOKIE` | `false` | Flaga `Secure` ciasteczka refresh (ustaw `true` za HTTPS) |
| `FRONTEND_URL` | `http://localhost:5173` | Baza linków (np. reset hasła) |
| `CORS_ORIGINS` | `http://localhost:5173` | Dozwolone originy (gdy front nie idzie przez proxy) |
| `STORAGE_DIR` | `./uploads` | Katalog na zdjęcia (`LocalFileStorage`) |

## Testy

```bash
cd backend && ./mvnw verify          # JUnit 5 + Testcontainers (wymaga działającego Dockera)
cd frontend && npm run lint && npm run typecheck && npm test && npm run build
```

## Architektura

```
/backend    Spring Boot 3.5 (Java 21), Maven
/frontend   React 18 + TypeScript + Vite
/docs       Plan MVP
docker-compose.yml   PostgreSQL 16
```

### Backend

- Pakiety per moduł domenowy (`auth`, `user`, kolejne: `gym`, `equipment`, `exercise`, `plan`, `workout`),
  w każdym warstwy **controller → service → repository**, DTO (`dto/`) oddzielone od encji.
- `storage/` – interfejs `FileStorage` (implementacja `LocalFileStorage`, docelowo S3/MinIO), walidacja zdjęć
  po sygnaturze pliku (jpg/png/webp, maks. 5 MB, maks. 8000 px), miniatury 320 px (Thumbnailator, JPEG).
  Pliki serwowane publicznie przez `GET /api/v1/files/{id}` i `/thumbnail` (cache 1 rok).
- `exercise/` – biblioteka 67 ćwiczeń (seed w migracji V4), własne ćwiczenia siłowni, powiązania sprzęt↔ćwiczenie.
  Logika „co da się zrobić w tej siłowni” w czystej klasie `ExerciseAvailabilityResolver` (testy jednostkowe):
  ćwiczenie z masą ciała – zawsze; inaczej potrzebny dostępny sprzęt o wymaganym typie albo jawne powiązanie.
- `plan/` – plany (dni i pozycje jako agregat z kaskadą, soft delete). Każda modyfikacja zwraca cały plan.
  Pozycję można dodać tylko dla ćwiczenia dostępnego w siłowni planu na wybranym sprzęcie (422 `exercise_not_available`).
  Kolejność: `POST …/move?direction=UP|DOWN` (przyciski na mobile) lub `PUT …/order` (pełna lista id).
  Pozycja na sprzęcie usuniętym/oznaczonym jako usunięty ma `equipmentUnavailable=true`.
- `common/` – obsługa błędów (ProblemDetail, RFC 7807), `PageResponse`, `BaseEntity` (UUID + audyt), normalizacja tekstu.
- Baza: PostgreSQL 16, migracje **Flyway** (`src/main/resources/db/migration`), Hibernate w trybie `validate`.
- Dokumentacja API: springdoc-openapi → `/v3/api-docs`, Swagger UI → `/swagger-ui.html`.

**Uwierzytelnianie**

- Access token: JWT HS256, ważny 15 min, przesyłany w nagłówku `Authorization: Bearer`.
  Walidowany przez Spring Security OAuth2 Resource Server.
- Refresh token: losowy, 30 dni, w ciasteczku `gp_refresh` (`HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`).
  W bazie trzymamy tylko jego hash (SHA-256). Każde odświeżenie **rotuje** token; ponowne użycie
  zrotowanego tokenu (po 30 s oknie tolerancji na równoległe odświeżenia z kilku kart) unieważnia całą sesję.
- Zmiana i reset hasła unieważniają wszystkie sesje użytkownika.
- Role: `USER` (używana), `GYM_ADMIN`, `TRAINER` (przygotowane pod wersję płatną).

**Format błędów** – zawsze `application/problem+json`:

```json
{
  "type": "urn:gymplanner:error:validation_failed",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "code": "validation_failed",
  "errors": [{ "field": "email", "code": "Email", "message": "must be a well-formed email address" }]
}
```

`code` jest stabilnym identyfikatorem – frontend tłumaczy go przez i18n (`errors.<code>`).

### Frontend

- React Router (trasy w `src/app/router.tsx`, strażnicy `RequireAuth` / `PublicOnly`).
- TanStack Query do komunikacji z API, React Hook Form + Zod do formularzy.
- Stylowanie: CSS Modules + tokeny w `src/styles/global.css`, mobile-first, bez biblioteki komponentów.
- i18n: react-i18next, teksty w `src/i18n/locales/pl/translation.json`.
- Access token tylko w pamięci; po przeładowaniu strony sesja odtwarzana przez `POST /auth/refresh`.

**Klient API – decyzja:** typy TypeScript są **generowane z OpenAPI** (`openapi-typescript` → `src/api/schema.d.ts`),
a wywołania idą przez lekki `openapi-fetch` z własnym `fetch` (dokleja token, przy 401 jednorazowo odświeża sesję
i ponawia żądanie). Uzasadnienie: kontrakt ma jedno źródło prawdy (backend), więc typy nie rozjeżdżają się z API;
generujemy wyłącznie typy (zero ciężkiego wygenerowanego kodu runtime), a hooki TanStack Query piszemy ręcznie –
czytelne i łatwe do zmiany. `schema.d.ts` jest commitowany, więc frontend buduje się bez działającego backendu.

Regeneracja typów po zmianie API:

```bash
cd backend && ./mvnw verify      # OpenApiSpecExportTest zapisuje target/openapi.json
cd frontend && npm run gen:api
```
