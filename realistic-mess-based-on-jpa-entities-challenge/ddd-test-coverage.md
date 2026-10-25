# DDD Test Coverage — realistic-mess-based-on-jpa-entities-challenge

| | |
|---|---|
| Status | **Awaiting user approval for commit** (Phase 2 gate) |
| Source of truth | `ddd-domain-analysis.md` (BR-01 … BR-34, 3 Bounded Contexts + Shared Kernel) |
| Produced by | DDD Test Coverage Guardian — Phase 2 of the Hexagonal Architecture Migration Orchestrator |
| Date | 2026-10-25 |

## 1. Audit Summary

- **Baseline:** 83 tests in 6 classes, all passing. Assertions were reviewed, not just test names.
- **Main weaknesses found:**
  - The detail fee table (BR-20) was asserted for `OUR` only.
  - The search fee table (BR-19) did not cover its fallback branch or rounding.
  - Ordering (BR-05) was tested for instant payments only.
  - Exact-vs-contains IBAN/BIC semantics, date-only bounds, malformed inputs, re-ingestion timestamps, JSON null handling, scheme default and consumer groups had no tests.
- **Result:** tests were added only. No existing assertion was changed or removed, and no production code was changed. New tests are characterization tests: they lock in **current** behavior, including the locked quirks.
- **Correction of the domain analysis:** the tests showed that inferred rules **BR-14 and BR-17 were wrong**. Malformed date/id filters and malformed detail ids return **500**, not 400. The `IllegalArgumentException` is wrapped by `@Repository` exception translation into `InvalidDataAccessApiUsageException`, which reaches the generic handler. `ddd-domain-analysis.md` has been updated (rules, C-04, new risk R-08, change history).
- BR-13, BR-20 (NPE → 500), BR-29 and BR-30 were inferred and are now **confirmed** by tests.

## 2. Test Suites and Conventions

| Class | Tier | Infrastructure | Baseline → now |
|---|---|---|---|
| `service/PaymentServiceUnitTest` | unit (Mockito) | none | 14 → 38 |
| `kafka/KafkaListenerUnitTest` | unit (Mockito) | none | 10 → 29 |
| `controller/PaymentControllerWebTest` | web slice (`@WebMvcTest`, `@MockitoBean`) | none | 14 → 24 |
| `util/PaymentUtilsTest` | unit | none | 17 → 17 |
| `repository/PaymentRepositoryUnitTest` | `@DataJpaTest` + Testcontainers Postgres | Docker | 12 → 28 |
| `integration/PaymentApplicationIntegrationTest` | `@SpringBootTest` + `@EmbeddedKafka` + Testcontainers | Docker | 16 → 31 |
| **Total** | | | **83 → 167** |

Counts include parameterized invocations.

**Conventions followed:**
- Existing `subject_scenario_outcome` camelCase method names.
- Tests placed in the per-type sections of each class.
- JUnit 5 `@ParameterizedTest` with display names for fee tables, value sets and boundaries.
- Kafka test ids in the reserved `00000000-0000-0000-0000-0000000009xx` range (908, 909, 910).
- Awaitility polling, no sleeps.
- `@MockitoBean`, and the `EntityManagerFactory`/`EntityManager` mocks in the web slice, kept as-is.

## 3. Coverage Matrix

Status: **Covered** · **Partially covered** · **Not covered** · **Not applicable**. *Added* = test introduced in this phase.

### Payment Processing

| Rule | Implementation | Unit / slice tests | Integration tests | Status | Remaining gap |
|---|---|---|---|---|---|
| BR-03 inbound limit uncapped | `InboundPaymentRepository.search` | `PaymentRepositoryUnitTest.inboundSearch_limitAbove200_isNotCapped` *(added; also asserts default 50)*, `inboundSearch_appliesFiltersAndDefaultLimit` | — | Covered | |
| BR-06 instant IBAN contains, case-insensitive | `InstantPaymentRepository.search` | `instantSearch_ibanFilters_matchAnySubstringIgnoringCase` *(added)*, `instantSearch_appliesAllFilters_andFormatsExecutionDatetime` | — | Covered | |
| BR-07 instant IBAN validation 15–34 | `InstantPaymentService.search`, `PaymentUtils.isValidIban` | `PaymentServiceUnitTest.instantSearch_ibanFilterWithin15To34CharsIgnoringSpacesOrBlank_isAccepted` *(added, 4 cases)*, `instantSearch_ibanFilterOutside15To34Chars_isRejected` *(added, 3 cases)*, `instantSearch_invalid*Iban_throws`, `PaymentUtilsTest.isValidIban_*` | `instantSearch_invalidIban_returns400WithMessage` *(added)* | Covered | |
| BR-09 BIC filters | `Inbound/OutboundPaymentRepository.search` | `inboundSearch_creditorIbanIsExact_originBicMatchesStoredValueIgnoringCase` *(added)*, `outboundSearch_debtorIbanIsExact_destinationBicDoesNotMatchLowercaseStoredValue` *(added)*, `outboundSearch_appliesParameterizedClauses` | — | Covered | |
| BR-10 endToEndId exact | `InstantPaymentRepository.search` | `instantSearch_appliesAllFilters_andFormatsExecutionDatetime` | `kafkaIngestion_isVisibleThroughSearchApi` | Covered | |
| BR-11 / BR-12 instant date bounds | `parseTimestampValue`, `parseEndOfDay` | `instantSearch_dateOnlyBounds_coverTheWholeDay` *(added)*, `instantSearch_unparseableExecutionBounds_areIgnored` *(added)* | — | Covered | |
| BR-18 outbound details contract | `OutboundPaymentController.get` | `PaymentControllerWebTest.outboundDetails_returnsSnakeCasePayloadWithAuditTimestamps` *(added)* | `outboundDetails_exposesSnakeCaseContractWithAuditTimestamps` *(added)*, `outboundDetails_returnsPayment` | Covered | |
| BR-19 search fee | `OutboundPaymentService.estimateFee` | `outboundSearch_feeEstimate_followsSearchFeeTable` *(added, 9 cases: OUR rounding, BEN, SHA, unknown, empty, null, null amount)*, `outboundSearch_computesFeePerChargesType`, `outboundGetById_mapsRequestedExecutionDateAsIsoDate` *(added)* | `outboundFee_searchAndDetailsApplyDifferentFeeTables` *(added)* | Covered | |
| BR-20 detail fee | `OutboundPaymentController.get` | `outboundDetails_feeEstimate_followsDetailFeeTable` *(added, 6 cases)*, `outboundDetails_ourWithoutAmount_returns500` *(added)*, `outboundDetails_computesFeeFromController` | `outboundFee_searchAndDetailsApplyDifferentFeeTables` *(added — seeded SHA: search 0.30 vs details 0.50; seeded OUR 15600.75: 156.01 vs 312.02)* | Covered | |
| BR-21 ageInDays | `InboundPaymentController.search`, `InboundPaymentService.getById` | `inboundGetById_computesAgeInDays`, `inboundGetById_futureValueDate_hasNegativeAgeInDays` *(added)*, `inboundGetById_withoutValueDate_leavesAgeInDaysNull` *(added)*, `PaymentControllerWebTest.inboundSearch_computesAgeInDays`, `inboundSearch_futureOrMissingValueDate_ageInDaysNegativeOrNull` *(added)* | `inbound_searchAndDetails_computeAgeInDaysFromValueDate` *(added)* | Covered | Uses the real clock (R-03); could flake if a run straddles midnight. |
| BR-27 scheme default | `InstantPaymentListener` | `instant_onMessage_missingOrNullScheme_defaultsToSctInst` *(added, 2 cases)* | — | Covered | |

### Compliance Hold

| Rule | Implementation | Unit / slice tests | Integration tests | Status | Remaining gap |
|---|---|---|---|---|---|
| BR-13 heldFrom/heldTo legacy format | `SuspendedPaymentRepository.search`, `PaymentUtils.parseTimestamp` | `suspendedSearch_heldBoundNotInLegacyTimestampFormat_returnsNoRows` *(added)*, `suspendedSearch_appliesParameterizedFilters`, `PaymentUtilsTest.parseTimestamp_*` | — | Covered | |
| BR-34 releasedAt / settlement consistency | not enforced | — | — | Not applicable | assumed vocabulary |

### Recall / Investigations

| Rule | Implementation | Unit / slice tests | Integration tests | Status | Remaining gap |
|---|---|---|---|---|---|
| BR-04 fixed limit 50 | `RecallRepository.search` | `recallSearch_alwaysUsesHardcodedLimitOf50` | — | Covered | |
| BR-11 / BR-12 recall date bounds | `RecallRepository.parseTimestampValue` | `recallSearch_dateOnlyInitiatedTo_excludesThatDay` *(added)*, `recallSearch_unparseableInitiatedBounds_areIgnored` *(added)* | — | Covered | |
| BR-15 recall type validation | `RecallService.search` | `recallSearch_knownTypeIgnoringCaseAndSpacesOrBlank_isAccepted` *(added, 4 cases)*, `recallSearch_invalidType_messageEchoesRequestedValue` *(added)*, `recallSearch_invalidType_throws`, `PaymentControllerWebTest.recallsSearch_invalidType_returns400` | `recallsSearch_unknownType_returns400` | Covered | |
| BR-28 original payment reference | `RecallListener` | `recall_onMessage_missingOrInvalidOriginalPaymentId_isDropped` *(added, 2 cases)* | `kafkaIngestion_recallReferencingUnknownPayment_isPersisted` *(added)* | Covered | |

### Shared Kernel and cross-context rules

| Rule | Implementation | Unit / slice tests | Integration tests | Status | Remaining gap |
|---|---|---|---|---|---|
| BR-01 blank ignored; trim + uppercase exact filters | all repositories | lowercase inputs in `instantSearch_appliesAllFilters_*`, `suspendedSearch_appliesParameterizedFilters`, `inboundSearch_appliesFiltersAndDefaultLimit`, `outboundSearch_appliesParameterizedClauses`, `recallSearch_appliesFilters` | `*Search_applies*Filter` (5) | Covered | |
| BR-02 default 50 / cap 200 | `PaymentUtils.limitOrDefault` | `PaymentUtilsTest.limitOrDefault_*`, `instantSearch_withoutLimit_returnsAtMost50Rows` *(added)*, `outboundSearch_limitAbove200_isCappedAt200` *(added)* | — | Covered | Suspended uses the same helper; not exercised separately. |
| BR-05 ordering | all repositories | `instantSearch_ordersByExecutionDatetimeDescending`, `suspendedSearch_ordersByHeldAtDescending`, `inboundSearch_ordersByValueDateThenCreatedAtDescending`, `outboundSearch_ordersByRequestedExecutionDateDescending`, `recallSearch_ordersByInitiatedAtDescending` *(4 added)* | — | Covered | |
| BR-08 exact IBAN filters | suspended / inbound / outbound repositories | `suspendedSearch_debtorIbanFilter_isTrimmedExactAndCaseSensitive` *(added)*; partial-IBAN-no-match in the two BIC tests above *(added)* | — | Covered | Trim and case-sensitivity are asserted for suspended only. |
| BR-14 malformed date/id filter → 500 | repositories + `@Repository` translation + `GlobalExceptionHandler` | — | `search_malformedDateOrIdFilter_returns500` *(added, 3 cases)* | Covered | Must stay integration-level: the translation only happens in the Spring context (R-08). |
| BR-16 unknown id → 404 | controllers | `*Details_foundAndMissing`, `outboundDetails_missing_returns404` | `details_unknownId_returns404` | Covered | |
| BR-17 malformed id → 500 / outbound 404 | `*Repository.findById`, `OutboundPaymentController.get` | `outboundDetails_malformedId_returns404WithoutQuerying` *(added)* | `details_malformedId_returns500` *(added, 4 cases)*, `outboundDetails_malformedId_returns404` *(added)* | Covered | |
| BR-22 rendering formats | `PaymentUtils.FMT/DAY_FMT`, mappers | `instantSearch_appliesAllFilters_andFormatsExecutionDatetime`, `suspendedSearch_mapsAllFields`, `suspendedGetById_foundAndMissing`, `recallSearch_mapsFieldsAndFormatsDates`, `inboundSearch_appliesFiltersAndDefaultLimit`, `outboundGetById_mapsRequestedExecutionDateAsIsoDate` *(added)* | — | Covered | |
| BR-23 upsert by id; invalid id dropped | all listeners | `*_onMessage_persistsRow`, `instant_onMessage_invalidOrMissingId_isDropped` *(added, 2 cases)* | `kafkaIngestion_persistsAllFivePaymentTypes`, `kafkaIngestion_upsertsBySameId` | Covered | |
| BR-24 created_at reset on re-delivery | all listeners | `instant_onMessage_setsCreatedAtAndUpdatedAtToIngestionTime` *(added)* | `kafkaIngestion_redelivery_resetsCreatedAt` *(added)* | Covered | |
| BR-25 failures logged and dropped | all listeners | `*_onMessage_malformedJson_swallowed` (5), `outbound_onMessage_persistenceFailure_swallowed` *(added)* | `kafkaIngestion_malformedMessage_isSkippedAndNextMessageIsConsumed` *(added)* | Covered | No-retry under auto-commit is not asserted (configuration only). |
| BR-26 dedicated consumer groups | `@KafkaListener` | `listener_consumesItsTopicWithDedicatedConsumerGroup` *(added, 5 cases)* | — | Covered | Asserts the annotation, not broker-side group isolation. |
| BR-29 `""` / `"null"` strings | all listeners | `instant_onMessage_missingStringFieldsAreEmpty_nullStringFieldsAreLiteralNull_missingAmountIsZero` *(added)*; date part: `suspended_onMessage_nullReleasedAt_staysNull`, `inbound_onMessage_nullSettlementDate_staysNull` | — | Partially covered | String behavior asserted for the instant listener only; the other four share the same `asText()` code. |
| BR-30 missing amount → 0 | all listeners | same test *(added)* | — | Partially covered | Instant listener only. |
| BR-31 date/timestamp parsing | listener `dateOf` / `timestampOf` | `instant_onMessage_executionDateTime_stripsTAndZWithoutTimezoneConversion` *(added, 3 cases)*, `inbound_onMessage_dateTimeValues_areTruncatedToDate` *(added)*, `recall_onMessage_dateOnlyInitiatedAt_defaultsToMidnight` *(added)* | `kafkaIngestion_recallReferencingUnknownPayment_isPersisted` (`Z` timestamp) | Covered | |
| BR-32 / BR-33 value sets | not enforced | — | — | Not applicable | assumed vocabulary; the migration must not add validation |

**Summary:** 29 Covered, 2 Partially covered (BR-29, BR-30), 0 Not covered, 3 Not applicable (BR-32 … BR-34).

## 4. Bounded Context Isolation

- No test crosses a context boundary except the deliberate Recall → Processing reference (BR-28), which is tested on the Recall side only (a reference to an unknown payment is persisted).
- **AC-08 limitation:** the suite is still organized **by technical layer** (one class per layer, one section per payment type), not by Bounded Context. Moving tests into per-context packages would be a restructuring of existing tests. It is deferred to Phase 5, where tests will move with each migrated context.

## 5. Execution Evidence

| Step | Command (from module dir) | Result |
|---|---|---|
| Baseline | `mvn test` | 83 tests, 0 failures, 0 errors |
| New + existing unit/web | `mvn test -Dtest='PaymentServiceUnitTest,KafkaListenerUnitTest,PaymentControllerWebTest,PaymentUtilsTest'` | 108 tests, 0 failures |
| Repository slice (Docker) | `mvn test -Dtest=PaymentRepositoryUnitTest` | 28 tests, 0 failures |
| Integration (Docker) — first run | `mvn test -Dtest=PaymentApplicationIntegrationTest` | 31 tests, **7 failures**: expected 400, got 500 (inferred BR-14 / BR-17 were wrong) |
| Integration — after correcting the expectations to observed behavior | same | 31 tests, 0 failures |
| Full suite ×2 (flakiness check) | `mvn test` twice | 167 tests, 0 failures, 0 errors, 0 skipped — both runs |

## 6. Full-Suite Result

| Class | Run 1 | Run 2 |
|---|---|---|
| `PaymentControllerWebTest` | 24 / 0 failures | 24 / 0 failures |
| `PaymentApplicationIntegrationTest` | 31 / 0 failures | 31 / 0 failures |
| `KafkaListenerUnitTest` | 29 / 0 failures | 29 / 0 failures |
| `PaymentServiceUnitTest` | 38 / 0 failures | 38 / 0 failures |
| `PaymentUtilsTest` | 17 / 0 failures | 17 / 0 failures |
| `PaymentRepositoryUnitTest` | 28 / 0 failures | 28 / 0 failures |
| **Total** | **167 passed** | **167 passed** |

ERROR log lines during the runs come from tests that deliberately send malformed input (expected).

## 7. Files Changed

| File | Change |
|---|---|
| `src/test/java/.../service/PaymentServiceUnitTest.java` | +97 lines (tests added) |
| `src/test/java/.../kafka/KafkaListenerUnitTest.java` | +123 lines |
| `src/test/java/.../controller/PaymentControllerWebTest.java` | +95 lines |
| `src/test/java/.../repository/PaymentRepositoryUnitTest.java` | +211 lines |
| `src/test/java/.../integration/PaymentApplicationIntegrationTest.java` | +128 lines |
| `ddd-domain-analysis.md` | BR-13/14/17/20/29/30 evidence and confidence updated; C-04, R-08, change history |
| `ddd-test-coverage.md` | new (this file) |

No production code changed. No test removed, skipped, disabled or weakened.

## 8. Known Limitations

- BR-21 tests use the real clock (no injectable clock exists in production code).
- BR-29 / BR-30 are asserted on one listener out of five.
- BR-25's "never retried" semantics and BR-26's broker-side isolation are configuration facts, not asserted behavior.
- Test organization by context is deferred (AC-08).
