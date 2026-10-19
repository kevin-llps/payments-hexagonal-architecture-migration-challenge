# AGENTS.md — maximum-spaghetti-challenge

Module-level instructions. Read the repo-root `../AGENTS.md` first for the intentional-smells rule and shared data/test conventions. This file only covers this module.

## What this module is

Independent Spring Boot 3.5.7 / Java 21 app (entrypoint `fr.kevin.llps.payments.PaymentsApplication`), the deliberately worst variant. Its only real components are `web/PaymentController` → `mess/PaymentsGodService` (all search, details, mapping, persistence, stats) and `mess/EverythingKafkaListener` (all five topics). There are no repositories, entities, services, or DTOs.

## Commands

Run from this directory (no Maven wrapper, no lint/format/typecheck config):

- Full suite (needs Docker): `mvn test`
- Fast, no-Docker subset: `mvn test -Dtest='PaymentsGodServiceUnitTest,EverythingKafkaListenerUnitTest,PaymentControllerWebTest'`
- Single class: `mvn test -Dtest=PaymentsGodServiceUnitTest`
- Integration suite: `mvn test -Dtest=PaymentApplicationIntegrationTest`
- Run locally: `docker compose up -d && mvn spring-boot:run` (app 8081, Postgres 5434 / `payments_spaghetti`, Kafka 9093)

## Testing gotchas

- `PaymentsGodServiceUnitTest` resets the service's **static** state (`QUERY_CACHE`, `QUERY_COUNTS`, `RECENT_ERRORS`) in `@BeforeEach`; keep that reset when adding tests or state leaks across tests.
- `PaymentControllerWebTest` only mocks `PaymentsGodService`; controller behavior includes the overdue-recall `daysLeft` sign flip.
- `PaymentApplicationIntegrationTest` needs Docker (`@EmbeddedKafka` + Testcontainers), asserts seeded ids (`...0101`, `E2E-INST-0001`), and cleans only ids prefixed `00000000-0000-0000-0000-0000000009`. Keep new Kafka test ids in that range and poll with Awaitility.

## Persistence / data gotchas

- `data.sql` starts with `DELETE FROM` on all five tables on every app/test-context startup. Local restarts wipe manually inserted rows.
- `schema.sql` is the only DDL source; `CREATE TABLE IF NOT EXISTS` means schema edits must be made there.
- Search and details return `Map`/`List<Map<String,Object>>`, not DTOs, with computed fields added by the god service (`estimatedFee`, recall `daysLeft`, warnings, etc.). Don't assume a typed response.

## Behavior quirks (intentional)

- `PaymentsGodService` builds all SQL by string concatenation; `sanitize()` only strips `'` and `;` (not real injection protection). Do not harden or refactor during unrelated work.
- Static mutable `QUERY_CACHE`/`QUERY_COUNTS`/`RECENT_ERRORS` are shared by all threads and requests (cache TTL 30s, hard limit 500, default limit 100). `/api/v1/internal/stats` exposes them.
- Details use `/api/v1/payments/{type}/{id}` with aliases (`instant`/`instant-payment`/`sct-inst`, `suspended`, `inbound`, `outbound`, `recall`/`recalls`); unknown types → 400, missing rows → 404.
- `/api/v1/payments/export?type=...` uses a raw JDBC connection and **never closes it** (intentional leak); unknown types fall back to `instant_payments`.
- `EverythingKafkaListener` uses the same group id `spaghetti-consumer` for all five topics and delegates to `godService.ingest`; both layers log-and-swallow failures, and a missing payload `id` becomes a random UUID.
