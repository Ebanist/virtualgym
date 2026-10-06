# CLAUDE.md – konwencje projektu GymPlanner

Monorepo: `backend/` (Spring Boot 3.5, Java 21, Maven), `frontend/` (React 18 + TS + Vite), `docker-compose.yml` (Postgres 16).
Plan MVP i etapy: `docs/PLAN.md`. Uruchomienie i architektura: `README.md`.

## Komendy

```bash
docker compose up -d                         # Postgres na :5432
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # API na :8080 + seed (DevDataSeeder)
cd backend && ./mvnw verify                  # testy (Testcontainers – wymaga Dockera)
cd frontend && npm run dev                   # :5173, proxy /api -> :8080
cd frontend && npm run lint && npm run typecheck && npm test && npm run build
cd frontend && npm run gen:api               # typy TS z backend/target/openapi.json (po ./mvnw verify)
```

Po każdej zmianie: backend `./mvnw verify` i frontend lint + typecheck + test + build muszą przechodzić.

## Backend – konwencje

- Pakiet per moduł domenowy: `com.gymplanner.<moduł>`; w środku `XController`, `XService`, `XRepository`, encje, `dto/`.
- Warstwy: controller (HTTP, walidacja `@Valid`, mapowanie na DTO) → service (logika, `@Transactional`) → repository.
  Kontroler nie dotyka repozytoriów; encje nie wychodzą poza serwis (zawsze DTO, mapowanie ręczne `XDto.from(entity)`).
- DTO jako `record`. Pola odpowiedzi opcjonalne oznaczaj `@Schema(nullable = true)` (inaczej w TS są wymagane –
  patrz `OpenApiConfig`). Nazwy schematów odpowiedzi kończą się na `Dto` lub `Response`.
- Encje dziedziczą po `BaseEntity` (UUID, `createdAt`, `updatedAt`). Schemat wyłącznie przez Flyway
  (`V<n>__opis.sql`), Hibernate `ddl-auto=validate`. Nigdy nie edytuj zastosowanej migracji – dodaj nową.
- Błędy: rzucaj wyjątki z `common.error` (`NotFoundException` 404, `ForbiddenException` 403, `ConflictException` 409,
  `BusinessRuleException` 422, `UnauthorizedException` 401). Każdy ma stabilny `code` (snake_case) – dodaj tłumaczenie
  `errors.<code>` w `frontend/src/i18n/locales/pl/translation.json`.
- Zalogowany użytkownik: parametr `AuthUser authUser` w metodzie kontrolera (resolver z JWT).
- Zasób cudzy/niedostępny (np. plan innego użytkownika) → 404, nie 403 (nie ujawniamy istnienia).
- Listy paginowane zwracają `PageResponse<T>` (`page` od 0, `size`, `totalElements`, `totalPages`).
- Soft delete: kolumna `deleted_at` (sprzęt, plany).
- Czas: `Instant` + `TIMESTAMPTZ`, bieżący czas z wstrzykiwanego `Clock`.
- Tekst do wyszukiwania/duplikatów: `TextNormalizer.normalize` (małe litery, bez polskich znaków); w encji trzymamy
  kolumnę `normalized_*`, a podobieństwo liczymy przez `pg_trgm` (`similarity`).
- Autoryzacja „tylko członkowie siłowni”: `GymAccessService.requireMember(userId, gymId)` → 403 `gym_membership_required`.
- Dane dev: `dev/DevDataSeeder` (profil `dev`) – rozszerzaj przy nowych modułach, seed działa tylko na pustej bazie.

## Testy backendu

- Integracyjne dziedziczą po `support.AbstractIntegrationTest` (MockMvc + współdzielony kontener Postgres).
- Nie czyścimy bazy między testami – używaj unikalnych danych (`TestUsers.register` tworzy losowy e-mail).
- W profilu `test` okno tolerancji refresh tokenu = 0 s.

## Frontend – konwencje

- Funkcje biznesowe w `src/features/<moduł>/` (strony `XPage.tsx`, schematy Zod `schemas.ts`, style `*.module.css`, testy `*.test.tsx`).
- Wspólne komponenty w `src/components/`, infrastruktura aplikacji w `src/app/`, API w `src/api/`.
- API: tylko przez `api` z `src/api/client.ts` + `unwrap(...)`; typy z `components['schemas'][...]` w `schema.d.ts`
  (plik generowany – nie edytuj ręcznie). Zapytania przez TanStack Query (`useQuery`/`useMutation`).
- Wszystkie teksty UI przez `t('...')`; komunikaty walidacji Zod to klucze i18n (`validation.*`).
- Błędy API: `errorMessage(t, e)` dla komunikatu ogólnego, `applyFieldErrors(e, setError)` dla błędów pól.
- Style: CSS Modules + zmienne z `styles/global.css`; mobile-first, cele dotykowe min. 44px (`--tap`).
- Pliki z komponentami eksportują tylko komponenty (reguła oxlint `only-export-components`) – helpery w osobnych plikach.

## Decyzje

- **JWT**: access token w pamięci (15 min), refresh token w ciasteczku httpOnly SameSite=Strict, rotacja z wykrywaniem
  ponownego użycia (okno tolerancji 30 s na równoległe odświeżenia). CSRF wyłączony – API bezstanowe, ciasteczko
  ograniczone do `/api/v1/auth`.
- **Klient API**: typy generowane z OpenAPI (`openapi-typescript`) + `openapi-fetch`; hooki pisane ręcznie.
- **Ćwiczenie ↔ sprzęt**: globalny słownik typów sprzętu (dopasowanie automatyczne) + ręczne powiązania z konkretnym sprzętem.
- **Własne ćwiczenia** widoczne dla wszystkich członków siłowni, w której je dodano.
- **Sprzęt „usunięty z siłowni”** ustawia ręcznie dowolny członek (historia zmian); zgłoszenia są informacyjne.
- **Trening**: z dnia planu lub ad hoc.
- **Duplikaty siłowni**: `POST /gyms` zwraca 409 `gym_possible_duplicate` z `candidates`, gdy w tym samym mieście jest
  podobna nazwa (trigramy ≥ 0.4 lub zawieranie się nazw); klient ponawia z `confirmDuplicate=true`. Autor siłowni
  automatycznie zostaje jej członkiem.
- **Nawigacja**: na telefonie dolny pasek (fixed), od 768px w nagłówku.
- **Wersja płatna**: tylko pola w modelu (`Gym.claimedByOrganizationId/status`, `Equipment.source/verified`,
  `WorkoutPlan.visibility/authorTrainerId`, role `GYM_ADMIN`/`TRAINER`) – bez logiki.
- Zdjęcia `GET /api/v1/files/**` są publiczne (ładowane przez `<img>`), identyfikatory to losowe UUID.
