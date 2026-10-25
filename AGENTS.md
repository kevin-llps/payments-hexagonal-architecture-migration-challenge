# AGENTS.md

## What this repo is

Three **independent** Spring Boot 3.5.7 / Java 21 apps that are the *deliberate* starting point of a hexagonal-architecture migration exercise. Same domain and Kafka topics, three flavors of legacy debt (see `README.md` for the port/infra matrix, API surface, and full smell inventory per module).

The two `realistic-*` modules are near-identical copies; `maximum-spaghetti-challenge` has its own single-god-service structure. All use the same `fr.kevin.llps.payments` package, but they **share no code and have no inter-module dependencies**. Apply common changes (or deliberately skip them) in each module; do not extract shared modules or add cross-module imports unless asked. Each module has its own `AGENTS.md` with module-specific traps — read the one for the module you are editing.

## The one rule

The code smells are **intentional**. Do not "clean up", fix, deduplicate, or reformat legacy code unless the user explicitly asks. The README section "Known smells (intentional — do not fix, migrate)" is the spec of what must *not* silently change. Treat existing behavior (including quirks like conflicting fee logic between controller and service) as the acceptance criteria for any migration.

## Build & test commands

No lint, formatter, typecheck, codegen, or CI config exists — don't go looking. There is no Maven wrapper; use the installed `mvn`.

- All modules: `mvn test` from the repo root (root `pom.xml` is a pure aggregator). This includes the Docker-dependent integration suites.
- One module: `cd <module> && mvn test` — modules parent `spring-boot-starter-parent` directly, so `-pl`/`-am` from the root adds nothing. Exception: `realistic-mess-based-on-jpa-entities-challenge` is now a multi-module parent (see its `AGENTS.md`); run its commands from its own directory.
- One test class: `mvn test -Dtest=PaymentServiceUnitTest` (run from inside the module); use `-Dtest=PaymentApplicationIntegrationTest` for the integration suite.
- Test tiers:
  - Fast unit/web tests (`PaymentServiceUnitTest`, `KafkaListenerUnitTest`, `PaymentControllerWebTest`, `PaymentUtilsTest`): Mockito or `@WebMvcTest` slices with `@MockitoBean` services — run with **no Docker, no DB, no Kafka**. Use `@MockitoBean` (Spring Boot 3.4+), not the deprecated `@MockBean`.
  - Caveat: the JPA module's `PaymentRepositoryUnitTest` is actually `@DataJpaTest` + Testcontainers (`@AutoConfigureTestDatabase(replace = NONE)`) and **requires Docker**, despite the `*UnitTest` name.
  - `integration/PaymentApplicationIntegrationTest`: `@SpringBootTest` + `@EmbeddedKafka` + Testcontainers Postgres (`postgres:16-alpine`) — **requires Docker**. This is the expensive suite; use the fast tier for iteration and run Docker-backed tests before declaring done.

## Testing conventions (easy to break)

- `schema.sql` + `data.sql` run at every startup (`spring.sql.init.mode: always`, including tests); integration tests assert against the **seeded rows** (e.g. `endToEndId=E2E-INST-0001`). Changing seed data or table names breaks tests and README examples.
- Integration tests share one Spring context/DB; `@BeforeEach` cleans only rows whose id starts with `00000000-0000-0000-0000-0000000009`. New Kafka-ingestion test data must use that reserved `...09` prefix or it will leak across tests.
- Kafka ingestion is asserted with Awaitility polling against Postgres — tests are async by nature; don't replace with fixed sleeps.

## Architecture facts that aren't obvious

- `realistic-mess-challenge` and `maximum-spaghetti-challenge` use `spring-jdbc` (`JdbcTemplate`); `realistic-mess-based-on-jpa-entities-challenge` uses `spring-boot-starter-data-jpa` but repositories are **hand-written `EntityManager` classes with string-built JPQL** — there are no Spring Data JPA interfaces anywhere. Keep it that way unless the migration task says otherwise.
- In the JPA module `spring.jpa.hibernate.ddl-auto: none`: `schema.sql` is the only DDL source. Entity changes require matching `schema.sql` (and usually `data.sql`) updates.
- Kafka payloads are flat JSON keyed by an `id` (UUID) used as upsert key; topics (`pay.instant`, `pay.suspended`, `pay.inbound`, `pay.outbound`, `pay.recall`) auto-create on first message. `OutboundPaymentMessageConsumer` is the outbound topic listener despite the misleading name (intentional smell).
- Each app runs its own Postgres + Kafka via its own `docker-compose.yml` with distinct host ports (see README), so all three can run side by side. Running the app locally requires `docker compose up -d` in that module first; tests do not use the compose stack.
- `target/` dirs are gitignored but present locally; ignore them.
