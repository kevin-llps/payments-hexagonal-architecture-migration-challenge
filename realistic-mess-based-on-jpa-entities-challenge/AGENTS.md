# AGENTS.md — realistic-mess-based-on-jpa-entities-challenge

Module-level instructions. Read the repo-root `../AGENTS.md` first: the intentional-smells rule and shared data/test conventions still apply. This file only adds what is specific to this module.

## What this module is

Independent Spring Boot 3.5.7 / Java 21 app (entrypoint `fr.kevin.llps.payments.PaymentsApplication`), the JPA-entities flavor of the migration exercise. Flow: HTTP controllers → services → hand-written `EntityManager` repositories → entities. Kafka listeners bypass that flow and `em.merge` entities directly.

## Commands

Run from this directory (no Maven wrapper, no lint/format/typecheck config):

- Full module suite (needs Docker): `mvn test`
- Fast, no-Docker subset: `mvn test -Dtest='PaymentServiceUnitTest,KafkaListenerUnitTest,PaymentControllerWebTest,PaymentUtilsTest'`
- Single class: `mvn test -Dtest=PaymentServiceUnitTest`
- Docker-backed repository slice: `mvn test -Dtest=PaymentRepositoryUnitTest`
- Integration suite: `mvn test -Dtest=PaymentApplicationIntegrationTest`
- Run locally: `docker compose up -d && mvn spring-boot:run` (app 8082, Postgres 5435 / `payments_jpa`, Kafka 9094)

## Testing gotchas

- `PaymentRepositoryUnitTest` is **not** a pure unit test: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + Testcontainers Postgres. It needs Docker, unlike the other `*UnitTest`/`*WebTest` classes.
- `PaymentControllerWebTest` must keep its `@MockitoBean EntityManagerFactory`/`EntityManager` and the `lenient()` factory wiring; `OutboundPaymentController` injects `@PersistenceContext`, so removing them breaks the slice context.
- `PaymentApplicationIntegrationTest` uses `@EmbeddedKafka` and a Postgres Testcontainer, asserts seeded IDs such as `...0101` / `E2E-INST-0001`, and cleans only ids prefixed `00000000-0000-0000-0000-0000000009`. Keep new Kafka test ids in that range; poll with Awaitility, not sleeps.

## Persistence / data gotchas

- `spring.jpa.hibernate.ddl-auto: none`: `schema.sql` is the only DDL source. It uses `CREATE TABLE IF NOT EXISTS`, so entity changes require matching `schema.sql` updates (and often `data.sql`).
- `data.sql` is destructive: it starts with `DELETE FROM` on all five tables, then re-inserts seeds **on every app/test-context startup**. Local restarts wipe manually inserted rows; don't point this app at data you need to keep.
- Entities use **public mutable fields** (no getters/setters), explicit snake_case `@Column(name = ...)` mappings, and `java.sql.Timestamp`/`Date`. `@Id` has no generation strategy — the `id` must come from the payload.
- Repository return types are inconsistent and intentional: `InstantPaymentRepository`/`InboundPaymentRepository` return DTOs; `Suspended`/`Outbound`/`Recall` repositories return entities that services map. Don't normalize this as a drive-by refactor.

## Layering / behavior quirks (intentional)

- Repositories build JPQL strings and bind named parameters; there are no Spring Data JPA interfaces. Keep that style unless the task is the migration itself.
- `OutboundPaymentController.get` bypasses the service: it uses `EntityManager.find`, returns a snake_case `Map` (e.g. `instruction_id`) and its own fee table (OUR 2%, SHA 0.50, else 0). `OutboundPaymentService` uses different fees (OUR 1%, BEN 0, else 0.30). Both behaviors are asserted; don't unify them unless asked.
- `InboundPaymentController.search` adds `ageInDays` to each row; `InboundPaymentService.getById` also computes it. Output is date-dependent.
- Kafka listeners (`*Listener` plus the misnamed `OutboundPaymentMessageConsumer`) each declare their own `groupId` (`jpa-*-group`), ignoring `spring.kafka.consumer.group-id`, and swallow all parse/persistence exceptions after logging. `em.merge` is the upsert.
- `GlobalExceptionHandler` maps `IllegalArgumentException` to 400 and everything else to 500; `OutboundPaymentController.get` catches invalid UUIDs and returns 404.

## Java coding conventions

Agents and skills must read [docs/java-coding-conventions.md](docs/java-coding-conventions.md) before writing Java code. These conventions apply to new and migrated code only; intentional legacy smells stay untouched until migrated.
