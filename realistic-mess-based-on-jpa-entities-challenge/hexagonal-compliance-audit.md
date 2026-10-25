# Hexagonal Architecture Compliance Audit — realistic-mess-based-on-jpa-entities-challenge

- **Date:** 2026-10-25 · **Audited commit:** `a43488c`
- **Auditor:** independent agent run of `.claude/agents/hexagonal-architecture-compliance-auditor.md` (read-only; only `mvn clean install`, `dependency:tree`, `dependency:analyze-only` executed; `git status` unchanged afterwards)
- **Overall status: COMPLIANT** — Rules 1–4 pass, no confirmed dead code.

## 1. Project Overview

| Maven module | Role | Compile dependencies |
|---|---|---|
| `jpa-domain` | domain model + application services, `port.in` / `port.out` | none (test scope only: junit-jupiter, assertj-core, mockito-junit-jupiter) |
| `jpa-infrastructure-adapters` | 5 `Jpa*Adapter`, 5 mappers, 5 entities, `schema.sql`, `data.sql` | `jpa-domain`, `spring-boot-starter-data-jpa`, postgresql (runtime) |
| `jpa-client-adapters` | 5 controllers, 5 REST mappers, 5 DTOs, 5 Kafka listeners, `GlobalExceptionHandler`, `RestDateFormats` | `jpa-domain`, `spring-boot-starter-web`, `spring-kafka` |
| `jpa-bootstrap` | `PaymentsApplication`, 3 context `@Configuration`, `ClockConfiguration`, `application.yml` | all three |

Contexts: `shared.domain`, `recall`, `compliancehold`, `processing.instant`, `processing.inbound`, `processing.outbound`. Reference documents: `isolated-domain-design.md`, `technical-layer-design.md` (TD-06, TD-07), `hexagonal-migration-plan.md`, `AGENTS.md`.

## 2. Rule 1 — Domain Isolation: PASS

| Check | Evidence | Result |
|---|---|---|
| Domain POM has no dependency on bootstrap / adapter modules | `domain/pom.xml`: 3 test-scope deps; `dependency:tree -pl domain` test-only | PASS |
| No framework imports in domain main | only `java.util.*`, `java.time.*`, `java.math.*`; no `springframework`, `jakarta`, `javax`, `hibernate`, `kafka`, `jackson`, `lombok`, `adapter` | PASS |
| No `java.sql` in domain main | none | PASS |
| No cross-context imports except `shared.domain` | per-file import check per context / aggregate: none | PASS |
| Framework-free application layer (TD-02) | `*QueryService`, `Record*Service` unannotated; instantiated only in `bootstrap/.../config/*Configuration.java` | PASS |
| No cycles | reactor order domain → infrastructure → client → bootstrap | PASS |

## 3. Rule 2 — Ports and Adapters: PASS

- **Output ports:** all 10 implemented by the 5 `@Repository` `Jpa*Adapter`s (two ports each).
- **Input ports:** all 10 implemented by the application services, exposed as `@Bean`s returning the port interface type (`ProcessingConfiguration`, `ComplianceHoldConfiguration`, `RecallConfiguration`), invoked by the matching controller / listener (constructor injection on the interface).
- `technical-layer-design.md` §7 rules verified: driving adapters import only `port.in`, domain read types and `shared.domain` types; only `adapter.out.persistence` uses JPA (JPA is not even on the client-adapters classpath); services instantiated only in config classes; persistence adapters import only `port.out` and domain types.
- Runtime wiring proven by `PaymentApplicationIntegrationTest` (full context, `@EmbeddedKafka`, Postgres): 31 / 31.
- No bypass: the legacy `em.find` in the outbound controller and `em.merge` in listeners are gone.
- Approved choices (not violations): REST mappers read domain types (§4); `PaymentId.parse` in the REST adapter (AD-11); `java.sql` used in Kafka listeners and in `OutboundPaymentRestMapper.toDetails` to preserve BR-18 / BR-29 … BR-31; domain and application classes share packages (TD-07).

## 4. Rule 3 — Dead Code: PASS

Every main class is referenced or is a framework entry point (`@SpringBootApplication`, `@Bean`, `@RestController`, `@RestControllerAdvice`, `@KafkaListener`, `@Repository`). No unused imports or private methods. All 20 ports used. No legacy leftovers (`service.*`, `repository.*`, `util.PaymentUtils`, `dto.*SearchRequest` absent).

**Confirmed dead code:** none.

**Suspected (intentionally kept):**
1. `GlobalExceptionHandler.handleBadRequest(IllegalArgumentException)` (`client-adapters/.../shared/adapter/in/rest/GlobalExceptionHandler.java:18-21`): no production path reaches it now and no test exercises it; kept as preserved legacy mapping / safety net (plan I-0, `AGENTS.md`).
2. `RecallSearchCriteria.from(..., Integer requestedLimit)` and `RecallSearchQuery.limit`: accepted, ignored (`SearchLimit.fixed()`), encoding "recalls always 50".
3. `spring.kafka.consumer.group-id: jpa-payments` (`bootstrap/.../application.yml`): overridden by every `@KafkaListener` group id — documented legacy quirk (BR-26).

`dependency:analyze-only`: starter / runtime-driver / component-scan false positives and transitive "used undeclared" entries; no architectural defect.

## 5. Rule 4 — Full Maven Build: PASS

`mvn clean install` (module root, no skip flags) → exit code 0, BUILD SUCCESS (21 s).

| Module | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| jpa-domain | 375 | 0 | 0 | 0 |
| jpa-infrastructure-adapters (Testcontainers) | 28 | 0 | 0 | 0 |
| jpa-client-adapters | 64 | 0 | 0 | 0 |
| jpa-bootstrap (31 integration + 1) | 32 | 0 | 0 | 0 |
| **Total** | **499** | **0** | **0** | **0** |

Logged `ERROR … unexpected error` / `WARN … skipping` lines come from tests asserting the 500 and swallow-and-log behavior.

## 6. Overall Status: COMPLIANT

| Rule | Result |
|---|---|
| 1. Domain isolation | PASS |
| 2. Ports and adapters | PASS |
| 3. Dead code | PASS (3 suspected, intentional) |
| 4. Full Maven build | PASS |

## 7. Remediation Recommendations (optional — no violation)

- Executable architecture rules (e.g. ArchUnit) for §7 constraints, notably inside `jpa-domain` where the domain / application split is by convention (TD-07).
- Pin or remove the `IllegalArgumentException` → 400 handler.
- Drop or document the unused recall `requestedLimit`.
- Declare direct dependencies used only transitively (e.g. bootstrap's `org.springframework.boot.*`).

**User decision (2026-10-25):** no follow-up; the migration ends here.

## 8. Risks, Limitations, Open Questions

- Dead-code analysis is reference-based (no whole-program call graph); reflection / Jackson / JPA field access treated as live.
- Sibling modules `realistic-mess-challenge/` and `maximum-spaghetti-challenge/` out of scope.
- Behavioral equivalence with the legacy app rests on the test suites (including the unchanged integration suite).
- Open questions recorded: permanence of the IAE handler; the unused recall limit; `java.sql` in domain *tests* (conformance references — accepted as test-only); long-term acceptability of shared domain / application packages (TD-07).

Observations (style only): small helpers repeated per criteria / listener / mapper (consistent with "no generic base classes", §17); fully qualified entity names where entity and aggregate share a simple name; `jpa-domain` inherits `spring-boot-starter-parent` for dependency management only; recall reconstitution accepts a null `originalPaymentId` (approved I-1 refinement).
