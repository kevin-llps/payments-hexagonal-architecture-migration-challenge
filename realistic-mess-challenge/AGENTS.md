# AGENTS.md — realistic-mess-challenge

Module-level instructions. Read the repo-root `../AGENTS.md` first for the intentional-smells rule and shared data/test conventions. This file only covers this module.

## What this module is

Independent Spring Boot 3.5.7 / Java 21 app (entrypoint `fr.kevin.llps.payments.PaymentsApplication`), the `JdbcTemplate` flavor. Flow: controllers → services → `@Repository` classes with string-built SQL; Kafka listeners write directly with `JdbcTemplate`.

## Commands

Run from this directory (no Maven wrapper, no lint/format/typecheck config):

- Full suite (needs Docker): `mvn test`
- Fast, no-Docker subset: `mvn test -Dtest='PaymentServiceUnitTest,KafkaListenerUnitTest,PaymentControllerWebTest,PaymentRepositoryUnitTest,PaymentUtilsTest'`
- Single class: `mvn test -Dtest=PaymentServiceUnitTest`
- Integration suite: `mvn test -Dtest=PaymentApplicationIntegrationTest`
- Run locally: `docker compose up -d && mvn spring-boot:run` (app 8080, Postgres 5433 / `payments_realistic`, Kafka 9092)

## Testing gotchas

- Only `PaymentApplicationIntegrationTest` needs Docker (`@SpringBootTest` + `@EmbeddedKafka` + Testcontainers). `PaymentRepositoryUnitTest` here mocks `JdbcTemplate` and needs no DB.
- `PaymentControllerWebTest` must keep `@MockitoBean JdbcTemplate` alongside the five service mocks; `OutboundPaymentController` injects it directly.
- Integration tests assert seeded ids (`...0101`, `E2E-INST-0001`) and clean only ids prefixed `00000000-0000-0000-0000-0000000009`. Keep new Kafka test ids in that range and poll with Awaitility.

## Persistence / data gotchas

- `data.sql` starts with `DELETE FROM` on all five tables on every app/test-context startup. Local restarts wipe manually inserted rows.
- `schema.sql` is the only DDL source (no JPA/Hibernate); `CREATE TABLE IF NOT EXISTS` means schema changes need explicit SQL edits.
- Search repositories differ: `InstantPaymentRepository`/`InboundPaymentRepository` return DTOs; `Suspended`/`Outbound`/`Recall` return `model/*Row` objects that services map. Don't normalize this as a drive-by refactor.
- `EmptyResultDataAccessException` is intentionally swallowed in `findById` methods and treated as "not found".

## Layering / behavior quirks (intentional)

- SQL is string-concatenated, including Kafka listener INSERTs; this is the documented smell, not something to fix during unrelated work.
- `OutboundPaymentController.get` bypasses the service and queries `JdbcTemplate` itself, returning the snake_case row plus `estimatedFee` using OUR 2% / SHA 0.50 / else 0. `OutboundPaymentService` uses OUR 1% / BEN 0 / else 0.30. Both are asserted; don't unify unless asked.
- `InboundPaymentController.search` adds `ageInDays` to each row; output is date-dependent.
- Kafka listeners (`*Listener` plus the misnamed `OutboundPaymentMessageConsumer`) each declare a `realistic-*-group` id, ignore `spring.kafka.consumer.group-id`, upsert with `INSERT ... ON CONFLICT (id) DO UPDATE`, and log-and-swallow failures.
- `GlobalExceptionHandler` maps `IllegalArgumentException` to 400 and everything else to 500.
