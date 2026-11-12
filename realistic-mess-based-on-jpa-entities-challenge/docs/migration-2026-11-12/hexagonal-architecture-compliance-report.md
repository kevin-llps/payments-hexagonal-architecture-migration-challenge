# Hexagonal Architecture Compliance Report: realistic-mess-based-on-jpa-entities-challenge

- **Phase:** 6 (Hexagonal Architecture Compliance Audit) of the orchestrated migration
- **Auditor:** `hexagonal-architecture-compliance-auditor` (independent audit: every claim below was checked against the code and a fresh build, not taken from the migration log)
- **Branch / commit audited:** `hexagonal-architecture-migration-2` @ `d2cf67a` (working tree: only the orchestrator's `agents-skills-execution-plan.md` modified and `prompts/` untracked, both out of scope and untouched)
- **Baseline for behaviour comparison:** `142a3a2` (307 tests)
- **References:** `ddd-domain-analysis.md`, `ddd-test-coverage.md`, `isolated-domain-design.md`, `technical-layer-design.md`, `hexagonal-migration-plan.md`, `../../AGENTS.md`, `../../../AGENTS.md`, `../java-coding-conventions.md`, and the user-approved Phase 5 decisions listed in the audit brief (treated as the approved target).

---

## Audit Round 1 (2026-11-12)

### 1. Project Overview

| Item | Finding | Evidence |
|---|---|---|
| Parent POM | `packaging=pom`, parent `spring-boot-starter-parent` 3.5.7, 4 modules, plain artifactIds | `pom.xml:21-26` |
| `domain` | 81 main source files in `domain.sharedkernel` and `domain.<ctx>` (`inbound`, `instant`, `outbound`, `recall`, `suspended`), each with `command`, `query`, `port.in`, `port.out` | `domain/src/main/java/fr/kevin/llps/payments/domain/**` |
| `client-adapters` | 5 REST controllers + response mappers, 2 `@RestControllerAdvice`, 5 Kafka listeners | `client-adapters/src/main/java/fr/kevin/llps/payments/adapter/in/{rest,kafka}/**` |
| `infrastructure-adapters` | 5 `Jpa*RepositoryAdapter` + 5 `Jpa*Mapper`, 5 unchanged entities in `fr.kevin.llps.payments.entity`, `schema.sql`, `data.sql` | `infrastructure-adapters/src/main/**` |
| `bootstrap` | `PaymentsApplication`, 6 `@Configuration` classes in `bootstrap.config`, `application.yml`, integration test, 6 ArchUnit classes | `bootstrap/src/**` |
| Legacy packages | none left (`controller`, `service`, `repository`, `kafka`, `dto`, `util` absent from every `src/main/java`) | `find */src/main/java -type d` |

Intended architecture (approved): `client-adapters → domain`, `infrastructure-adapters → domain`, `bootstrap → all`; model ring = aggregates + `command` + `query` + shared kernel; output ports = domain-service ring; `*Service` + `port.in` = application ring; adapters outermost.

### 2. Commands Run

All from the module directory unless stated.

| # | Command | Exit | Result |
|---|---|---|---|
| 1 | `mvn -B clean verify` (Docker available) | **0** | BUILD SUCCESS, 39 s. Reactor: parent, domain, client-adapters, infrastructure-adapters, bootstrap all SUCCESS |
| 2 | `mvn -B -q dependency:tree -DoutputFile=… -DappendOutput=true` | 0 | see §3 |
| 3 | `git diff --stat -M 142a3a2 HEAD -- '*schema.sql' '*data.sql' '*application.yml' docker-compose.yml` | 0 | 3 pure renames (100 % similarity), 0 insertions, 0 deletions |
| 4 | `git diff -M 142a3a2 HEAD -- '*PaymentApplicationIntegrationTest.java'` | 0 | only listener type swaps + 1 added characterization test (no assertion changed) |
| 5 | repo root `mvn -B -q validate` | 0 | no artifactId clash with the other two apps |
| 6 | grep / Python reference scans (forbidden imports, cross-context imports, unreferenced classes / methods / constants) | 0 | see §4–§7 |

Test counts from command 1 (0 failures, 0 errors, 0 skipped everywhere):

| Module | Tests | Notes |
|---|---|---|
| domain | 254 | 31 classes, plain JUnit 5 / AssertJ / Mockito |
| client-adapters | 169 | 5 Kafka listener tests, 7 REST slice / handler tests, formatter test (41) |
| infrastructure-adapters | 70 | `@DataJpaTest` + Testcontainers Postgres, mapper tests, `JpaSchemaConstraintsTest` |
| bootstrap | 118 | `PaymentApplicationIntegrationTest` 88 + ArchUnit 30 (`DomainIsolationTest` 6, `AdapterDependenciesTest` 8, `PortsTest` 5, `HexagonalArchitectureTest` 2, `CyclesTest` 1, `CodingConventionsTest` 8) |
| **Total** | **611** | matches the Phase 5 final figure |

### 3. Rule 1: Domain Isolation: **COMPLIANT**

| Check | Result | Evidence |
|---|---|---|
| Domain POM has no production dependency | yes | `domain/pom.xml`: only `junit-jupiter`, `assertj-core`, `mockito-junit-jupiter`, all `test` scope; `dependency:tree` for `domain` shows only test-scope artifacts |
| Domain does not depend on `bootstrap`, `client-adapters`, `infrastructure-adapters` | yes | `dependency:tree`: `domain` has no module dependency; ArchUnit AR-04 (outer layers) GREEN |
| No framework / JDBC / Jackson / Kafka imports in domain | yes | grep of every `import` in `domain/src/main/java`: all are `java.lang/util/math/time` or `fr.kevin.llps.payments.domain..`; grep for fully-qualified `org.springframework`, `jakarta`, `java.sql`, `hibernate`, `jackson`, `kafka` in code: 0 hits (only Javadoc words "adapter") |
| No framework annotations in domain | yes | grep `@[A-Z]` in domain main: only `@Override` |
| Bounded contexts only share the shared kernel | yes | every cross-package domain import targets `domain.sharedkernel` (e.g. `instant/query/InstantPaymentSearchCriteria.java` → `CodeFilter`, `Iban`, `SearchLimit`, `TimestampBound`); AR-02 and AR-03 GREEN |
| Domain model does not depend on `*Service` or ports | yes | AR-04 (model) GREEN; AR-06 onion GREEN |
| Dependencies point inward | yes | `client-adapters → domain`, `infrastructure-adapters → domain`, `bootstrap → domain, client-adapters, infrastructure-adapters`; nothing depends on `bootstrap` |

### 4. Rule 2: Ports and Adapters: **COMPLIANT**

**Ports (25 approved, 25 found).** 15 input ports (`Search<X>`, `Get<X>Details`, `Persist<X>` × 5 contexts) and 10 output ports (`Load<X>s`, `Save<X>` × 5) in `domain.<ctx>.port.in` / `.port.out`, all interfaces (AR-14 GREEN).

| Check | Result | Evidence |
|---|---|---|
| Every input port has exactly one `*Service` implementation, framework-free | yes | 15 `*Service` classes, e.g. `domain/.../outbound/GetOutboundPaymentDetailsService.java:9` `implements GetOutboundPaymentDetails`; AR-05, AR-14 GREEN |
| Every output port is implemented by a JPA adapter | yes | `JpaOutboundPaymentRepositoryAdapter.java:26` `implements LoadOutboundPayments, SaveOutboundPayment`; same pattern for the 4 other contexts; AR-13 ×2 GREEN |
| REST controllers call input ports only | yes | e.g. `RestOutboundPaymentController.java:24-25` fields `SearchOutboundPayments`, `GetOutboundPaymentDetails`; `RestInboundPaymentController.java:21-22`; no `*Service` / `port.out` import in `client-adapters` (import census); AR-08 GREEN |
| Kafka listeners call input ports only | yes | e.g. `KafkaOutboundPaymentListener.java:34,45` `PersistOutboundPayment.persist(...)`; all 5 listeners inject only `Persist<X>`; AR-08 GREEN |
| Driven adapters do not use `*Service` / `port.in` | yes | `infrastructure-adapters` imports only aggregates, `query.*Criteria`, `port.out`, shared kernel (import census); AR-09 GREEN |
| No adapter bypasses the hexagon | yes | `EntityManager` and `entity..` only in `adapter.out.persistence` (AR-11 ×2 GREEN, no legacy scope left); the legacy `OutboundPaymentController` `EntityManager` bypass is gone: details go `RestOutboundPaymentController.get` → `GetOutboundPaymentDetails` → `LoadOutboundPayments` |
| Adapters are independent | yes | AR-10 GREEN (REST ↮ Kafka ↮ persistence) |
| Everything is wired | yes | 15 input-port `@Bean`s in `bootstrap/.../config/{Instant,Suspended,Inbound,Outbound,Recall}UseCaseConfiguration.java` (plus `SearchFeePolicy`, `DetailsFeePolicy`, `Clock`); 5 `@Repository` adapters, 5 `@RestController`, 5 `@Component` listeners, 2 advices picked up by the `@SpringBootApplication` scan in `fr.kevin.llps.payments`; the full context starts in `PaymentApplicationIntegrationTest` (88 tests, real Postgres and embedded Kafka) |
| Representative flows traced | yes | (1) `GET /api/v1/outbound-payments/search` → `RestOutboundPaymentController.search:46-48` → `SearchOutboundPaymentsService.search:22-27` (`OutboundPaymentSearchCriteria.from`, `SearchFeePolicy`) → `LoadOutboundPayments.search` → `JpaOutboundPaymentRepositoryAdapter.search:38-71`. (2) `pay.outbound` → `KafkaOutboundPaymentListener.onMessage:42-58` → `PersistOutboundPaymentService.persist:22-35` (`Clock`) → `SaveOutboundPayment.save` → `em.merge` (`JpaOutboundPaymentRepositoryAdapter.java:82`) |

**Business rules not misplaced in infrastructure.** Verified by reading every adapter and grepping for rule literals (`50`, `200`, fee constants, `OUR`/`SHA`/`BEN`, `ChronoUnit`, `now(`, IBAN checks):
- Limits: `SearchLimit.capped/uncapped/fixedDefault` in the domain criteria (`RecallSearchCriteria.java:61` fixed 50; `InboundPaymentSearchCriteria.java:41` uncapped; others capped). The JPA adapters only pass `criteria.limit().value()` to `setMaxResults`.
- Fees: `SearchFeePolicy` (OUR 1 %, BEN 0, else 0.30, no amount 0) and `DetailsFeePolicy` (OUR 2 %, SHA 0.50, else 0, OUR without amount → `FeeNotComputableException`) in `domain.outbound`; C-01 divergence intentionally preserved.
- `ageInDays`: `InboundPayment.ageInDaysOn(LocalDate)` (`InboundPayment.java:95-98`) called by both inbound use cases with the injected `Clock`; the REST mapper only renders it.
- IBAN validation, recall-type check, date-bound grammars, negative limit: domain criteria and shared kernel.
- What remains in adapters is technical translation: JPQL text, `LIKE` wildcards, `ORDER BY`, legacy `Timestamp.valueOf`/`Date.valueOf` payload grammar in listeners (approved: "parsing code kept identical in the adapter", TLD §10), JSON shapes and the outbound details UTC formatter (approved Q5 = b).

### 5. Rule 3: Dead Code: **COMPLIANT (no confirmed dead code); 2 suspected items**

Method: Python scan of all 126 main source files (domain 81, client-adapters 23, infrastructure-adapters 15, bootstrap 7) for (a) classes not referenced by any other main class, (b) methods and constants with no use in main code other than their declaration; every hit then checked by hand.

- Classes with no main-code reference: only framework entry points (5 `Rest*Controller`, 2 `Rest*ExceptionHandler`, 5 `Kafka*Listener`, 5 `Jpa*RepositoryAdapter`, 6 `@Configuration`, `PaymentsApplication`). All are registered through component scanning or `@Bean` and exercised by the integration test. **Not dead.**
- `@ExceptionHandler` methods: invoked by Spring MVC. **Not dead** in principle; see S-01.
- No legacy class, DTO, `PaymentUtils`, unused port, unused `@Bean` or unused dependency was found. `spring-tx` in `client-adapters` is used by `@Transactional` on the 5 listener methods.

Suspected dead code (needs user confirmation, see the validation gate):

| ID | Item | Evidence | Why only suspected |
|---|---|---|---|
| S-01 | `RestGlobalExceptionHandler.handleBadRequest` (`client-adapters/.../adapter/in/rest/RestGlobalExceptionHandler.java:20-23`) and `handleInvalidDataAccessApiUsage` (`:30-36`) | After the migration, every input error is a domain exception handled first by `RestDomainExceptionHandler` (`@Order(HIGHEST_PRECEDENCE)`, `RestDomainExceptionHandler.java:21-53`); the only `IllegalArgumentException` source left in main code is caught and translated in `PaymentId.java:26`; negative limits are rejected before `setMaxResults`. No production path producing an `IllegalArgumentException` or an `InvalidDataAccessApiUsageException` with an IAE cause was found. Only `RestGlobalExceptionHandlerTest` (unit) reaches them. | Spring MVC or Hibernate can still raise an `IllegalArgumentException` in cases not covered by static analysis. The user approved moving the handler with its body unchanged (IT-07), so keeping it as a safety net is a legitimate choice. |
| S-02 | `TimestampBound.absent()` (`domain/.../sharedkernel/TimestampBound.java:48-50`) | public static factory, 0 uses in main code, 4 uses in `TimestampBoundTest` | It may be meant as public shared-kernel API. It is small and harmless. |

### 6. Rule 4: Full Maven Build: **COMPLIANT**

`mvn -B clean verify`: **exit 0**, **611 tests, 0 failures, 0 errors, 0 skipped**, all 5 reactor projects SUCCESS, including the Docker-backed `infrastructure-adapters` suite and `PaymentApplicationIntegrationTest`. No tests were skipped and no failure-hiding flag was used. The only console warning is the JDK notice about Mockito's dynamically loaded byte-buddy agent, which has no effect on the build. Log: `/private/tmp/claude-501/-Users-kevinllps-projects-exalt-it-payments-hexagonal-architecture-migration-challenge-realistic-mess-based-on-jpa-entities-challenge/4b88aa05-ed46-474f-a3b7-8478f20c0b9b/scratchpad/build.log`.

### 7. Compliance against Phases 3–4 (as amended in Phase 5)

| Design item | Status | Evidence |
|---|---|---|
| DR-1 framework-free domain | met | §3; AR-01, AR-01b GREEN |
| DR-2 contexts share only the shared kernel / DR-3 kernel independent | met | AR-02, AR-03 GREEN; import census |
| DR-4 no outer-layer dependency / DR-6 model ↛ services, ports | met | AR-04 ×2, AR-06 GREEN |
| 4 Maven modules, plain artifactIds, approved dependency graph (TL-DR-8) | met | §1, `dependency:tree` |
| Per-context layout (`domain.<ctx>`, `command`, `query`, `port.in`, `port.out`, no `usecase`) | met | directory listing; AR-14 ×5 GREEN |
| Port naming `Persist<X>.persist`, `Load<X>s`, `Save<X>` | met | e.g. `PersistOutboundPayment.java:7`, `LoadOutboundPayments.java`, `SaveOutboundPayment.java:8` |
| TL-DR-1/2 adapter dependency rules (commands and queries allowed for all adapters) | met | AR-08, AR-09 GREEN; JPA adapters import `query.*Criteria`, REST imports `query.*Query`, Kafka imports `command.*Command` |
| TL-DR-4 adapters independent | met | AR-10 GREEN |
| TL-DR-5/6 entities and `EntityManager` only in persistence | met | AR-11 ×2 GREEN, no legacy scope |
| TL-DR-9 no `java.sql` in REST | met | AR-12 GREEN; `java.sql` only in `adapter.in.kafka` (payload grammar) and `adapter.out.persistence` |
| TL-DR-10 `@Transactional` only on Kafka listener methods | met | AR-18 ×2 GREEN; 5 occurrences, all `Kafka*Listener.onMessage` |
| Naming `Rest*`, `Kafka*`, `Jpa*` | met | AR-16 ×3 GREEN |
| Error contract: `RestDomainExceptionHandler` highest precedence, `RestGlobalExceptionHandler` body unchanged, `FeeNotComputableException` without handler (generic 500) | met | `RestDomainExceptionHandler.java:21-22`; `RestGlobalExceptionHandler.java:13-49`; `FeeNotComputableException.java:3-7` |
| Outbound details: UTC formatter, invalid id → 404, two fee policies | met | `RestOutboundPaymentDetailsFormatter.java:29-38`; `RestOutboundPaymentController.java:57-62`; `SearchFeePolicy` / `DetailsFeePolicy` |
| `KafkaOutboundPaymentListener` rename, same topic and group | met | `KafkaOutboundPaymentListener.java:40` `topics = "pay.outbound", groupId = "jpa-outbound-group"` |
| Feature-preservation matrix (TLD §10) | met | URLs, topics and the 5 `jpa-*-group` group ids unchanged; `application.yml`, `schema.sql`, `data.sql` byte-identical to baseline; integration assertions unchanged |
| AR-19 not added; BR-OUT-10, BR-SHR-07 retired; 400 instead of 500 for six rules | approved decisions, not deviations | brief; `hexagonal-migration-plan.md` §6 IT-07, §11 |
| No temporary ArchUnit exception left | met | no `LEGACY_*` scope in `AdapterDependenciesTest` or `CodingConventionsTest` |
| Circular dependencies | none | AR-15 (`fr.kevin.llps.payments.(*)..` free of cycles) GREEN; Maven graph acyclic; intra-context package cycles (model ↔ `query`) accepted by design |

### 8. Migration Acceptance Criteria (MAC-01..MAC-08)

| MAC | Verdict | Evidence |
|---|---|---|
| MAC-01 no behaviour regression | **met** | 611/611 green; `PaymentApplicationIntegrationTest` assertions identical to baseline `142a3a2` (diff = listener type swaps plus 1 added characterization test) |
| MAC-02 no silent test loss | **met** | 611 ≥ 307; every removed legacy test has a remap entry or an approved retirement (`hexagonal-migration-plan.md` §11 remap log; spot-checked `SearchFeePolicyTest`, `DetailsFeePolicyTest`, `KafkaInstantPaymentListenerTest.should_ignore_api_style_execution_datetime_field_name_in_events`, `RestErrorContractTest`, `RestGlobalExceptionHandlerTest`) |
| MAC-03 conflicts C-01..C-06 preserved | **met** | C-01 fee tests in domain and integration; C-02 `should_return_404_with_empty_body_when_outbound_details_id_is_not_a_uuid` vs `should_return_400_with_invalid_uuid_message_when_details_id_is_not_a_uuid` (integration); C-03 limit policies in domain criteria; C-06 listener field-name test; all green |
| MAC-04 error contract | **met** | `max-results cannot be negative`, `Bad request`, invalid-UUID and 500 bodies asserted in integration, REST slice, and `RestErrorContractTest` tests; all green |
| MAC-05 ingestion semantics | **met** | 5 `Kafka*ListenerTest` (53 tests) + Kafka integration tests with Awaitility; no `Thread.sleep` in any test |
| MAC-06 seeds and schema untouched | **met** | `git diff` of `schema.sql`, `data.sql`, `application.yml` vs baseline: renames only, 0 line changes |
| MAC-07 test relocation by context | **met** | tests sit in the module and package of the code they test (domain `<ctx>` / `query`, adapter `<ctx>` packages) |
| MAC-08 domain tests infrastructure-free | **met** | domain POM has no Spring, Testcontainers or Kafka on any classpath; grep for `SpringBootTest`, `WebMvcTest`, `Testcontainers`, `Docker` in `domain/src/test`: 0 hits |

### 9. Findings by Severity

| Severity | ID | Finding | Evidence | Status |
|---|---|---|---|---|
| Critical | — | none | — | — |
| High | — | none | — | — |
| Medium | — | none | — | — |
| Low | S-01 | Suspected unreachable legacy handlers in `RestGlobalExceptionHandler` (IAE → 400, `InvalidDataAccessApiUsageException` → 400/500) | `RestGlobalExceptionHandler.java:20-36`; `PaymentId.java:26`; `RestDomainExceptionHandler.java:21-53` | pending user confirmation |
| Low | S-02 | `TimestampBound.absent()` used only by tests | `TimestampBound.java:48-50` | pending user confirmation |
| Low | D-01 | The traceability matrix in `hexagonal-migration-plan.md` §8 (BR-API row) names a `RestDomainExceptionHandlerTest` that does not exist. The handler is covered by the 5 `Rest*ControllerTest` slices (each imports it) and by the integration tests. | `grep -rl RestDomainExceptionHandler */src/test` → 5 controller tests | documentation only |
| Low | D-02 | Root `README.md` still describes the JPA module's pre-migration layout and smells (line 9: "controller still bypasses the service layer"; line 129: `OutboundPaymentController`, `OutboundPaymentMessageConsumer`). The root and module `AGENTS.md` were updated, the README was not. | `../README.md:9,129` | documentation only; the README is the exercise statement, so updating it is optional |
| Info | I-01 | The legacy payload helpers (`timestampOf`, `dateOf`) are duplicated across the 5 Kafka listeners | e.g. `KafkaInstantPaymentListener` and `KafkaRecallListener` | approved: per-listener legacy grammar kept identical (TLD §10) |
| Info | I-02 | `ChargesType` / `RecallType` are records over `String`, not enums (the conventions say "enums for closed sets") | `ChargesType.java:7`, `RecallType.java:8` | justified: open vocabulary (BR-SHR-10, OQ-01) |
| Info | I-03 | The transaction boundary is on the driving adapter (`@Transactional` on `Kafka*Listener.onMessage`), not on the use case | 5 listeners, line 41/43 | approved TL-DR-10 (keeps BR-ING-06 commit-time propagation) |
| Info | I-04 | No ArchUnit rule asserts that every output port has an implementation, or every input port a `*Service`. Today this is guaranteed by Spring context startup in the integration test, not at rule level. | `PortsTest`, `AdapterDependenciesTest` | acceptable; optional hardening |

No architectural violation (Rules 1–4) was found.

### 10. Overall Compliance Status

| Rule | Status |
|---|---|
| Rule 1: Domain isolation | COMPLIANT |
| Rule 2: Ports and adapters | COMPLIANT |
| Rule 3: Dead code | COMPLIANT (0 confirmed; 2 suspected, Low) |
| Rule 4: Full Maven build | COMPLIANT (exit 0, 611 tests) |

**Overall: COMPLIANT.**
**Provisional final status: PASS WITH EXCEPTIONS.** The exceptions are the user-approved deviations from the original design (AR-19 not added, BR-OUT-10 and BR-SHR-07 retired, the 400-instead-of-500 change) and four open Low items (S-01, S-02, D-01, D-02), none of which is an architecture violation. If the user accepts S-01 and S-02 as intentional and the documentation items as optional, the status can be raised to **PASS**.

### 11. Remediation Recommendations (optional, none blocking)

1. **S-01:** either keep `RestGlobalExceptionHandler` unchanged as a safety net (current approved state), or remove the two IAE-related handlers and their 5 unit tests in `RestGlobalExceptionHandlerTest`, keeping `handleGeneric`. Removal needs explicit approval because the handler was approved as "moved, body unchanged".
2. **S-02:** keep `TimestampBound.absent()` as shared-kernel API, or reduce its visibility / inline `ABSENT` in the tests.
3. **D-01:** change the BR-API row of `hexagonal-migration-plan.md` §8 to list the 5 `Rest*ControllerTest` classes instead of `RestDomainExceptionHandlerTest`, or add a dedicated `RestDomainExceptionHandlerTest`.
4. **D-02:** optionally add a note in `../README.md` that the JPA module on this branch has been migrated.
5. **I-04 (hardening):** optionally add an ArchUnit rule requiring each `port.out` interface to have an implementation in `adapter.out.persistence`.

### 12. Risks and Limitations

- The dead-code analysis is static (reference scan plus manual review). Reachability of S-01 through framework-internal `IllegalArgumentException`s cannot be proven or disproven statically.
- The auditor did not re-run the `docker compose up -d && mvn spring-boot:run` smoke test. Wiring is verified by the full-context integration test (real Postgres Testcontainer and embedded Kafka).
- Approved accepted risks still apply: pre-1583 Kafka timestamps skipped, one clock reading per search request, rendering of years above 9999.
- Some integration assertions depend on the date (`ageInDays`). They passed on 2026-11-12.
- Integration tests use `@EmbeddedKafka`, not a real broker. Production broker settings (`application.yml`) are byte-identical to baseline.

### 13. Validation Gate (Round 1)

Pending user answers (asked one at a time by the orchestrator):

1. S-01: keep or remove the two IAE-related handlers of `RestGlobalExceptionHandler`?
2. S-02: keep `TimestampBound.absent()` as public shared-kernel API?
3. D-01 / D-02: fix the documentation inconsistencies?
4. Do you want me to address any of the identified findings?

The final status is recorded here once the answers are in.

**Answers (2026-11-12, relayed verbatim by the orchestrator):** 1. S-01 (a): keep both handlers as a safety net; closed as intentional (accepted exception). 2. S-02 (a): keep `TimestampBound.absent()` as public shared-kernel API; closed as intentional. 3. D-01/D-02 (c): fix D-01 and D-02 (JPA module only). 4. (b): apply the documentation fixes as a docs-only remediation, then re-audit. Fixes applied by the migration agent and committed with user approval as `1bd479e`. See Round 2.

---

## Audit Round 2 (2026-11-12): re-audit after the Phase 6 remediation

- **Commit audited:** `1bd479e` ("docs: fix migration plan traceability and README for migrated JPA module"). It is the only commit after `d2cf67a`.
- **Scope:** check D-01 and D-02 independently, confirm the remediation touched no code, record the user decisions on S-01 and S-02, and set the final status.

### R2.1 Commands Run

| # | Command | Exit | Result |
|---|---|---|---|
| 1 | `git log --oneline d2cf67a..HEAD` | 0 | 1 commit: `1bd479e` |
| 2 | `git show --stat 1bd479e` | 0 | 2 files: `README.md` (+2/−2), `docs/migration-2026-11-12/hexagonal-migration-plan.md` (+12/−1) |
| 3 | `git diff --name-only d2cf67a HEAD -- '*.java' '*.xml' '*.yml' '*.sql' '*.properties'` | 0 | **0 files**: no source, test, build, config or seed change |
| 4 | `git diff --quiet d2cf67a HEAD -- maximum-spaghetti-challenge realistic-mess-challenge` (repo root) | 0 | the other two apps are unchanged |
| 5 | `git show 1bd479e` (full diff review) | 0 | see R2.2 |
| 6 | grep of `RestDomainExceptionHandler` / `isBadRequest` in the 5 `Rest*ControllerTest` classes | 0 | every slice imports the handler and asserts 400 (inbound 4, instant 3, outbound 3, recall 3, suspended 2 assertions) |
| 7 | `mvn -B clean verify`. A rebuild was not strictly needed because no code changed; it was run for direct evidence at HEAD. | **0** | BUILD SUCCESS; **611 tests, 0 failures, 0 errors, 0 skipped**: domain 254, client-adapters 169, infrastructure-adapters 70, bootstrap 118 (88 integration + 30 ArchUnit). Same per-module counts as Round 1. |

### R2.2 Remediation Verification

| Finding | Verdict | Evidence |
|---|---|---|
| D-01 | **closed** | `hexagonal-migration-plan.md:262`: the BR-API-01..09 row now lists `RestInstantPaymentControllerTest`, `RestSuspendedPaymentControllerTest`, `RestInboundPaymentControllerTest`, `RestOutboundPaymentControllerTest`, `RestRecallControllerTest`, `RestGlobalExceptionHandlerTest`, `RestErrorContractTest`, plus `PaymentApplicationIntegrationTest`. It states explicitly that there is no dedicated `RestDomainExceptionHandlerTest`. Every listed class exists, and the 5 slices do load and assert the handler (command 6). A new §16 "Phase 6 Remediation" (R-1) records all four decisions. |
| D-02 | **closed** | `../README.md` line 9 (JPA row) and line 129 (JPA smells paragraph) each end with an italic "migrated on this branch" note. The original text is kept as a description of the starting point. The rows and paragraphs for `realistic-mess-challenge` and `maximum-spaghetti-challenge` are unchanged (diff limited to those 2 lines). |
| S-01 | **accepted exception** (user decision 1a) | `RestGlobalExceptionHandler.java:20-36` unchanged; kept as a safety net |
| S-02 | **accepted exception** (user decision 2a) | `TimestampBound.java:48-50` unchanged; public shared-kernel API |

Rules 1–4 were not re-scanned beyond the rebuild: no file under any `src/` and no POM changed since Round 1 (command 3). The 30 ArchUnit rules passed again in command 7.

### R2.3 Approved Exceptions (final list)

1. S-01: `RestGlobalExceptionHandler` IAE / `InvalidDataAccessApiUsageException` handlers kept as a safety net, although they are probably unreachable.
2. S-02: `TimestampBound.absent()` kept as public API, used only by tests.
3. AR-19 not added (user decision at the IT-07 gate).
4. BR-OUT-10 and BR-SHR-07 retired (approved).
5. Six input-error rules return 400 instead of 500 (Phase 2 P2-Q1 fix, approved behaviour change).
6. Accepted risks: pre-1583 Kafka timestamps skipped; clock read once per search request; rendering of years above 9999.

Informational items I-01 to I-04 from Round 1 stay as recorded: approved or justified design choices, none a violation. I-04 (no ArchUnit rule for "every output port has an implementation") remains optional hardening.

### R2.4 Final Status

| Rule | Status |
|---|---|
| Rule 1: Domain isolation | COMPLIANT |
| Rule 2: Ports and adapters | COMPLIANT |
| Rule 3: Dead code | COMPLIANT (0 confirmed; S-01, S-02 accepted as intentional) |
| Rule 4: Full Maven build | COMPLIANT (exit 0, 611 tests at `1bd479e`) |
| MAC-01..MAC-08 | all met (unchanged from Round 1; no code changed) |

**Overall: COMPLIANT. Final status: PASS WITH EXCEPTIONS.** The exceptions are the approved, documented items in R2.3. There are no open violations and no open findings.
