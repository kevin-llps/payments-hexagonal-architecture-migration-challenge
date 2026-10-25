# AGENTS.md — realistic-mess-based-on-jpa-entities-challenge

Module-level instructions. Read the repo-root `../AGENTS.md` first: the intentional-smells rule and shared data/test conventions still apply. This file only adds what is specific to this module.

## What this module is

Independent Spring Boot 3.5.7 / Java 21 app (entrypoint `fr.kevin.llps.payments.PaymentsApplication`), the JPA-entities flavor of the exercise, **migrated to hexagonal architecture** (see `hexagonal-migration-plan.md`, `isolated-domain-design.md`, `technical-layer-design.md`). The legacy behavior — quirks included — is preserved and remains the acceptance criteria.

This directory is a `pom` parent with four Maven modules:

| Module | Contents |
|---|---|
| `domain/` (`jpa-domain`) | No framework dependency. `shared.domain` (Shared Kernel), one package per context — `recall`, `compliancehold`, `processing.instant`, `processing.inbound`, `processing.outbound` — holding the aggregate, search criteria, policies and application services, with ports in `port.in` (use cases, queries, commands) and `port.out` (repositories) |
| `infrastructure-adapters/` (`jpa-infrastructure-adapters`) | `<context>.adapter.out.persistence`: `Jpa*Adapter`, mappers, JPA entities; `schema.sql` / `data.sql` in `src/main/resources` |
| `client-adapters/` (`jpa-client-adapters`) | `<context>.adapter.in.rest` (controllers, REST mappers, response DTOs), `<context>.adapter.in.kafka` (listeners), `shared.adapter.in.rest` (`GlobalExceptionHandler`, `RestDateFormats`) |
| `bootstrap/` (`jpa-bootstrap`) | `PaymentsApplication`, `application.yml`, `<context>.config` (bean wiring), `shared.config.ClockConfiguration` |

Dependency direction: adapters → domain, bootstrap → everything; the domain depends on nothing. Application services are plain classes wired in `<context>.config`.

## Commands

Run from this directory (no Maven wrapper, no lint/format/typecheck config). The parent POM sets `surefire.failIfNoSpecifiedTests=false` and skips `spring-boot:run` outside `bootstrap`, so the commands below work from here:

- Full suite (needs Docker): `mvn test`
- Fast, no-Docker subset (domain + web slices + listeners): `mvn test -pl domain,client-adapters`
- Docker-backed persistence adapters: `mvn test -pl domain,infrastructure-adapters`
- Single class: `mvn test -Dtest=RecallSearchCriteriaTest`
- Integration suite: `mvn test -Dtest=PaymentApplicationIntegrationTest`
- Run locally: `docker compose up -d && mvn spring-boot:run` (app 8082, Postgres 5435 / `payments_jpa`, Kafka 9094)

## Testing gotchas

- Tests live in the module of the code they test, in the same package. `domain` tests are plain JUnit / Mockito; `*CharacterizationTest` classes are the Phase 2 characterization tests of the legacy services, re-targeted to the query services.
- `client-adapters` web tests are `@WebMvcTest(controllers = X.class)` slices configured by the test-only `ClientAdaptersTestApplication`; they mock the input ports with `@MockitoBean`. `OutboundPaymentControllerWebTest` deliberately uses the real `OutboundPaymentQueryService` (both fee tariffs) with only the repository port mocked. The instant and outbound listener tests likewise use the real record services with mocked repository ports.
- `infrastructure-adapters` adapter tests extend `JpaAdapterTestSupport` (`@DataJpaTest` + one shared Postgres Testcontainer, **needs Docker**); `src/test/resources/application.yml` makes `schema.sql` / `data.sql` run as in the app, and every table is emptied before each test.
- `PaymentApplicationIntegrationTest` (bootstrap) uses `@EmbeddedKafka` and a Postgres Testcontainer, asserts seeded IDs such as `...0101` / `E2E-INST-0001`, and cleans only ids prefixed `00000000-0000-0000-0000-0000000009`. Keep new Kafka test ids in that range; poll with Awaitility, not sleeps. It is the end-to-end acceptance suite and was not modified by the migration.

## Persistence / data gotchas

- `spring.jpa.hibernate.ddl-auto: none`: `schema.sql` is the only DDL source. It uses `CREATE TABLE IF NOT EXISTS`, so entity changes require matching `schema.sql` updates (and often `data.sql`).
- `data.sql` is destructive: it starts with `DELETE FROM` on all five tables, then re-inserts seeds **on every app/test-context startup**. Local restarts wipe manually inserted rows; don't point this app at data you need to keep.
- Entities (persistence model only) use **public mutable fields**, explicit snake_case `@Column(name = ...)` mappings, and `java.sql.Timestamp`/`Date`. `@Id` has no generation strategy — the `id` must come from the payload. The domain never sees them: `*JpaMapper` converts. A payment row without amount cannot be reconstituted (decision D-01).
- JPA adapters build JPQL strings and bind named parameters; there are no Spring Data JPA interfaces. All filter decisions (trimming, case, match mode, date parsing, limits) are made by the domain `*SearchCriteria`; adapters only translate them.

## Behavior quirks (preserved, asserted — don't "fix" them unless asked)

- Two outbound fee tariffs: search uses `SearchFeeEstimate` (OUR 1 %, BEN 0, else 0.30), details use `DetailFeeEstimate` (OUR 2 %, SHA 0.50, else 0). Outbound details return a snake_case `Map` (e.g. `instruction_id`, `created_at`) and answer 404 for a non-UUID id (not trimmed); the other details endpoints answer 500 for a malformed id.
- Date / timestamp filters reproduce the legacy parsers exactly: `LenientTimestamp` (`Timestamp.valueOf`), `LenientDate` (`Date.valueOf`), `compliancehold.LenientIsoTimestamp` (lenient `SimpleDateFormat`); each has a conformance test against the JDK class it replaces. Instant date-only `executionTo` means 23:59:59, recall date-only `initiatedTo` means 00:00:00; a suspended held bound in another format matches nothing.
- Limits: default 50, capped at 200, except inbound (not capped) and recalls (always 50).
- `ageInDays` (inbound) is computed by `InboundPaymentQueryService` from the injected `Clock`; output is date-dependent.
- Kafka listeners (`*Listener` plus the misnamed `OutboundPaymentMessageConsumer`) each declare their own `groupId` (`jpa-*-group`), ignoring `spring.kafka.consumer.group-id`, and swallow all parse / persistence exceptions after logging. The upsert is `em.merge` in the JPA adapters; every ingestion resets `created_at` / `updated_at`.
- `GlobalExceptionHandler`: `InvalidSearchFilterException` and `IllegalArgumentException` → 400 with the message, everything else → 500 with a generic body.
