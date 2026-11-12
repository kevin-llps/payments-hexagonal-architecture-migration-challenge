# Payments — Hexagonal Architecture Migration Challenge

Three deliberately messy legacy payment applications, built as the **starting point** of a hexagonal architecture migration exercise. Same domain, same API surface, same Kafka topics — three flavors of technical debt.

| Folder | Style | Port |
|---|---|---|
| `realistic-mess-challenge/` | Realistically messy layered codebase: duplicated repositories/DTO mapping, static god-util, business logic in controllers, copy-pasted Kafka listeners | 8080 |
| `maximum-spaghetti-challenge/` | Strawman spaghetti: one ~1400-line god service, static mutable caches, string-concatenated SQL everywhere, one Kafka listener class for all topics | 8081 |
| `realistic-mess-based-on-jpa-entities-challenge/` | Same realistic mess, but backed by JPA entities: hand-written `EntityManager` repositories with dynamically built JPQL, entity↔DTO mapping duplication, `em.merge` upserts in listeners, controller still bypasses the service layer | 8082 |

## Branches

| Branch | Purpose |
|---|---|
| [`main`](https://github.com/kevin-llps/payments-hexagonal-architecture-migration-challenge/tree/main) | The untouched legacy codebases — the starting point of the challenge. |
| [`harness`](https://github.com/kevin-llps/payments-hexagonal-architecture-migration-challenge/tree/harness) | The AI harness on top of `main`, no migration applied yet: `AGENTS.md` files (repo rules and per-module traps), Java coding conventions, and the `.claude/` config with the hexagonal-migration agents (orchestrator, domain/technical design, …) and skills (SOLID design, testing, ArchUnit tests, skill-creator / skill-testing / skill-updater tooling). This is the reference branch where the harness keeps evolving. |
| [`demo-hexagonal-architecture-migration`](https://github.com/kevin-llps/payments-hexagonal-architecture-migration-challenge/tree/demo-hexagonal-architecture-migration) | Codebase used for the live demo at the **AI For Devs (Exalt IT)** meetup ([event page](https://luma.com/0upab1zy)), talk *"Migration vers l'Architecture Hexagonale : les Agents IA face au code spaghetti, mythe ou réalité ?"*. It is a snapshot of the `harness` branch, slightly simplified for the demo (skill dependency inversion removed), ready to run the migration live. |
| [`hexagonal-architecture-migration-1`](https://github.com/kevin-llps/payments-hexagonal-architecture-migration-challenge/tree/hexagonal-architecture-migration-1) | Result of a full migration run of `realistic-mess-based-on-jpa-entities-challenge/` driven by the agents: domain model and ports design docs, module split into `domain` / `adapters` / `bootstrap`, a shared kernel, each bounded context (instant, inbound, outbound, compliance hold, recall) migrated to hexagonal architecture, tests reorganized by context, and a final hexagonal compliance audit report. |
| [`hexagonal-architecture-migration-2`](https://github.com/kevin-llps/payments-hexagonal-architecture-migration-challenge/tree/hexagonal-architecture-migration-2) | Second full migration run of `realistic-mess-based-on-jpa-entities-challenge/`, started from `demo-hexagonal-architecture-migration`. It has DDD domain analysis and test-coverage docs and characterization tests for 75 business rules. The module is split into `domain` / `client-adapters` / `infrastructure-adapters` / `bootstrap` with a shared kernel, and the five bounded contexts were migrated over iterations IT-00..IT-07. The suite has 611 tests, including 30 ArchUnit rules. The branch ends with a compliance audit report (*PASS WITH EXCEPTIONS*) and a migration summary in `docs/migration-2026-11-12/`. |

## Domain

Five payment types, each with **its own data model, table, search API and Kafka topic**:

| Payment type | Table | Search API | Details API | Kafka topic |
|---|---|---|---|---|
| Instant payment | `instant_payments` | `GET /api/v1/instant-payments/search` | `GET /api/v1/payments/{type}/{id}`* | `pay.instant` |
| Suspended payment | `suspended_payments` | `GET /api/v1/suspended-payments/search` | `GET /api/v1/payments/{type}/{id}`* | `pay.suspended` |
| Inbound payment | `inbound_payments` | `GET /api/v1/inbound-payments/search` | `GET /api/v1/payments/{type}/{id}`* | `pay.inbound` |
| Outbound payment | `outbound_payments` | `GET /api/v1/outbound-payments/search` | `GET /api/v1/payments/{type}/{id}`* | `pay.outbound` |
| Recall | `recalls` | `GET /api/v1/recalls/search` | `GET /api/v1/payments/{type}/{id}`* | `pay.recall` |

\* In `realistic-mess-challenge/` and `realistic-mess-based-on-jpa-entities-challenge/` each type has its own details endpoint instead: `GET /api/v1/instant-payments/{id}`, `/suspended-payments/{id}`, `/inbound-payments/{id}`, `/outbound-payments/{id}`, `/recalls/{id}`. In `maximum-spaghetti-challenge/` details go through the generic `GET /api/v1/payments/{type}/{id}` (type = `instant|suspended|inbound|outbound|recall`).

Each search API exposes **different query parameters** mirroring the different data models, e.g.:

- instant: `endToEndId, debtorIban, creditorIban, status, currency, executionFrom, executionTo, limit`
- suspended: `debtorIban, holdReason, reviewStatus, heldFrom, heldTo, limit`
- inbound: `creditorIban, originBic, status, currency, valueDateFrom, valueDateTo, limit`
- outbound: `debtorIban, destinationBic, chargesType, status, executionFrom, executionTo, limit`
- recalls: `recallType, originalPaymentId, originalPaymentType, reasonCode, status, initiatedFrom, initiatedTo, limit`

Extra endpoints in `maximum-spaghetti-challenge/`: `GET /api/v1/payments/export?type=...` (CSV, leaks connections), `GET /api/v1/internal/stats` (exposes static counters/caches).

## Run

Prerequisites: Java 21, Maven, Docker.

### realistic-mess-challenge (port 8080, Postgres 5433, Kafka 9092)

```bash
cd realistic-mess-challenge
docker compose up -d
mvn spring-boot:run
```

### maximum-spaghetti-challenge (port 8081, Postgres 5434, Kafka 9093)

```bash
cd maximum-spaghetti-challenge
docker compose up -d
mvn spring-boot:run
```

### realistic-mess-based-on-jpa-entities-challenge (port 8082, Postgres 5435, Kafka 9094)

```bash
cd realistic-mess-based-on-jpa-entities-challenge
docker compose up -d
mvn spring-boot:run
```

Schema and seed data are applied automatically at startup (`schema.sql`, `data.sql`).

### Try the APIs

```bash
curl "http://localhost:8080/api/v1/instant-payments/search?status=COMPLETED"
curl "http://localhost:8080/api/v1/suspended-payments/search?reviewStatus=PENDING"
curl "http://localhost:8081/api/v1/payments/instant/00000000-0000-0000-0000-000000000101"
```

### Produce Kafka events

Each topic expects a flat JSON payload with an `id` (UUID) used as upsert key. All five topics are pre-created by the broker on first message. Example (against the realistic-mess-challenge Kafka on 9092):

```bash
docker exec -it payments-realistic-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic pay.instant
```

```json
{"id":"00000000-0000-0000-0000-000000000901","endToEndId":"E2E-INST-0099","uetr":"5f0a4e4e-1a1a-4a1a-9a1a-000000000099","amount":42.00,"currency":"EUR","debtorIban":"DE89370400440532013000","debtorName":"ACME GmbH","creditorIban":"NL91ABNA0417164300","creditorName":"M. Jansen","executionDateTime":"2026-10-17 10:00:00","status":"COMPLETED","scheme":"SCT INST"}
```

```json
{"id":"00000000-0000-0000-0000-000000000902","amount":5000.00,"currency":"EUR","debtorIban":"DE89370400440532013000","debtorName":"ACME GmbH","creditorIban":"FR7630006000011234567890189","creditorName":"SARL Dupont","holdReason":"SANCTIONS_SCREENING","complianceCaseRef":"CASE-2026-0099","reviewStatus":"PENDING","heldAt":"2026-10-17 11:00:00","releasedAt":null}
```

```json
{"id":"00000000-0000-0000-0000-000000000903","creditTransferRef":"CTR-2026-99901","amount":1234.56,"currency":"EUR","creditorIban":"DE89370400440532013000","creditorName":"ACME GmbH","originBic":"BNPAFRPP","valueDate":"2026-10-17","settlementDate":"2026-10-17","status":"SETTLED"}
```

```json
{"id":"00000000-0000-0000-0000-000000000904","instructionId":"OUT-2026-9901","amount":300.00,"currency":"EUR","debtorIban":"DE89370400440532013000","debtorName":"ACME GmbH","destinationBic":"ABNANL2A","requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"SENT"}
```

```json
{"id":"00000000-0000-0000-0000-000000000905","recallReference":"RCL-2026-9901","recallType":"R-COR","originalPaymentId":"00000000-0000-0000-0000-000000000904","originalPaymentType":"OUTBOUND","reasonCode":"AC04","amount":300.00,"currency":"EUR","initiatedAt":"2026-10-17 12:00:00","deadline":"2026-10-19","status":"INITIATED"}
```

Then verify persistence:

```bash
curl "http://localhost:8080/api/v1/instant-payments/00000000-0000-0000-0000-000000000901"
```

Or query Postgres directly:

```bash
docker exec -it payments-realistic-pg psql -U payments -d payments_realistic -c "SELECT id, end_to_end_id, status FROM instant_payments;"
```

## Known smells (intentional — do not fix, migrate)

**realistic-mess-challenge**: SQL string concatenation with user input; shared mutable `SimpleDateFormat` (`PaymentUtils.FMT`); duplicated row→DTO mapping in every repository/service; inconsistent layering (some repos return DTOs, `OutboundPaymentController` queries the DB itself, `InboundPaymentController` computes business fields); misnamed `OutboundPaymentMessageConsumer`; swallowed `EmptyResultDataAccessException` as control flow; hardcoded `LIMIT 50` for recalls; fee logic duplicated with *different* values in service vs controller.

**maximum-spaghetti-challenge**: `PaymentsGodService` does search, details, mapping, persistence and stats with 5× copy-pasted blocks; static mutable `QUERY_CACHE`/`QUERY_COUNTS`/`RECENT_ERRORS` shared by all threads; `sanitize()` as fake SQL-injection defense; `exportLegacyCsv` leaks JDBC connections; double exception swallowing (listener + god service); magic numbers everywhere (`CACHE_TTL_SECONDS`, `HARD_LIMIT`, fees); one Kafka listener class for all five topics.

**realistic-mess-based-on-jpa-entities-challenge**: hand-built JPQL in every repository (duplicated filter/limit/mapping logic, string-built queries with named parameters); entity↔DTO mapping duplicated between repositories and services; shared mutable `SimpleDateFormat`; listeners upsert with `em.merge` inside try/catch-all; `OutboundPaymentController` still bypasses the service layer with its own `EntityManager` query and its *own* fee table (2%/0.50 vs the service's 1%/0.30); misnamed `OutboundPaymentMessageConsumer`; hardcoded `LIMIT 50` for recalls; entities expose public mutable fields.
