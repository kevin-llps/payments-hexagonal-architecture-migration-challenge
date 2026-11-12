# DDD Test Coverage — realistic-mess-based-on-jpa-entities-challenge

- **Status:** Phase 2 gate answered on 2026-11-12 (P2-Q1: fix to 400; P2-Q2: keep test organization; P2-Q3: do not commit yet). Changes are **uncommitted**.
- **Produced by:** `ddd-test-coverage-guardian`, Phase 2 of the hexagonal-architecture migration.
- **Source of truth:** `docs/migration-2026-11-12/ddd-domain-analysis.md` (75 rules, FINAL).
- **Baseline:** branch `demo-hexagonal-architecture-migration`, committed tests at HEAD. Production code: **one approved change** in `GlobalExceptionHandler` (decision P2-Q1, §4).
- **Skills used:** `unit-test`.

---

## 1. Summary

| Item | Before (HEAD) | After (this phase) |
|---|---|---|
| Test classes | 6 | 7 (+ `GlobalExceptionHandlerTest`) |
| Executed tests (`mvn test`) | 83 | **307** (0 failures, 0 errors, 0 skipped) |
| Rules with at least one committed test | 46 / 75 | **75 / 75** |
| V-PT rules pinned by a characterization test | 0 / 11 | **11 / 11** |
| V-PT rules **confirmed as written** | — | 5 fully (BR-API-08, BR-ING-06, BR-ING-07, BR-SUS-04, BR-OUT-07) + the outbound part of BR-API-05 |
| V-PT rules whose observed behaviour differed from the analysis | — | 6 (BR-SHR-03, BR-API-05 (4 non-outbound types), BR-API-07, BR-INB-04 (invalid part), BR-OUT-04 (invalid part), BR-RCL-03 (invalid part)): **fixed to 400** by decision P2-Q1 and now confirmed (§4) |
| Open questions answered by tests | — | OQ-03 (`limit=0`), OQ-04 (outbound details date/time rendering) |

All new tests are **characterization tests**: they pin the current behaviour, intentional quirks and conflicts C-01..C-06 included. Where the analysis had inferred a 400 that the running application did not return (500), the user decided the 400 is intended (P2-Q1). The tests were rewritten to expect 400, seen failing, and made green by a minimal change in `GlobalExceptionHandler` (§4). No other production code was touched.

## 2. Existing test coverage assessment (HEAD baseline)

| Class | Tier | Tests | Strengths | Weaknesses found |
|---|---|---|---|---|
| `PaymentUtilsTest` | unit | 17 | IBAN length, limit policy, timestamp parsing, utility contract | No boundaries (14/15/34 chars, 200/201 limit), no negative/zero limit, no date-only timestamp |
| `PaymentServiceUnitTest` | unit (Mockito) | 14 | IBAN validation messages, fee table OUR/SHA/BEN, recall date formatting | Fee rounding and unknown/null charges type untested; recall type normalization and message untested; future/null value date untested |
| `KafkaListenerUnitTest` | unit (Mockito) | 10 | Happy-path mapping, malformed JSON swallowed | No missing/null field semantics (BR-ING-07), no `T`/`Z`/date-only parsing, no scheme default, no consumer group check, no recall `originalPaymentId` drop |
| `PaymentControllerWebTest` | web slice | 14 | 404 for unknown ids, 500 on failure, details fee OUR | Generic error body not asserted exactly; details fee table SHA/BEN/other/null amount untested; malformed outbound id, non-integer limit, non-GET untested |
| `PaymentRepositoryUnitTest` | `@DataJpaTest` + Testcontainers | 12 | Every filter on the happy path, hard-coded recall limit | No unparsable bounds, ordering (except instant), inbound no-cap limit, schema constraints, malformed date/UUID filters |
| `PaymentApplicationIntegrationTest` | `@SpringBootTest` + EmbeddedKafka + Testcontainers | 16 | Seeds, one filter per type, Kafka ingestion and upsert | Error contract through the real stack, C-01 fee divergence on seeds, outbound details payload, ingestion edge cases untested |

Assertion quality: existing assertions were kept as they are (none removed or weakened). Some existing tests combine several calls in one method (e.g. `*_foundAndMissing`); they were left untouched, and the new tests use one behaviour per test.

## 3. Business rule to test traceability matrix

Legend: **U** = `PaymentUtilsTest`, **S** = `PaymentServiceUnitTest`, **K** = `KafkaListenerUnitTest`, **W** = `PaymentControllerWebTest`, **H** = `GlobalExceptionHandlerTest`, **R** = `PaymentRepositoryUnitTest` (Docker), **I** = `PaymentApplicationIntegrationTest` (Docker). Test names are given without class prefix; (new) marks tests added in Phase 2.

Status values: **Covered**; **Covered (fixed P2-Q1)** for rules made to match the analysis by the approved fix (§4).

### 3.1 Shared Kernel

| Rule | Implementation | Unit tests | Integration tests | Status |
|---|---|---|---|---|
| BR-SHR-01 | entities, listeners | K `instant_onMessage_persistsRow`, `should_skip_instant_event_without_id` (new) | I `kafkaIngestion_upsertsBySameId` | Covered |
| BR-SHR-02 | `PaymentUtils.limitOrDefault` | U `limitOrDefault_*`, `should_cap_limit_at_200_and_pass_zero_and_negative_values_through` (new) | R `should_order_suspended_payments_by_held_at_descending_and_apply_limit` (new), `should_order_outbound_payments_by_requested_execution_date_descending_and_apply_limit` (new); I `should_return_most_recent_instant_payment_first_within_limit` (new) | Covered |
| BR-SHR-03 (V-PT) | `limitOrDefault` + `setMaxResults` + `GlobalExceptionHandler` | U (new, -1 and 0 pass through); R `should_reject_negative_instant_limit_with_illegal_argument_root_cause` (new), `should_return_no_instant_payment_when_limit_is_zero` (new) | I `should_return_400_when_limit_is_negative` (new, 4 types, body `{"error":"max-results cannot be negative"}`), `should_return_empty_list_when_limit_is_zero` (new, 4 types) | **Covered (fixed P2-Q1)** |
| BR-SHR-04 | `PaymentUtils.isValidIban` | U `isValidIban_*`, `should_accept_iban_only_when_length_without_spaces_is_between_15_and_34` (new), `should_accept_iban_of_any_characters_since_no_checksum_or_country_check_exists` (new); S `should_accept_iban_filter_containing_spaces_when_length_without_spaces_is_valid` (new) | I `should_return_400_with_message_when_instant_debtor_iban_is_invalid` (new) | Covered |
| BR-SHR-05 | `FMT`/`DAY_FMT`, mappers | S `suspendedSearch_mapsAllFields`, `recallSearch_mapsFieldsAndFormatsDates`, `should_map_suspended_details_with_same_fields_as_search` (new) | R `instantSearch_appliesAllFilters_andFormatsExecutionDatetime`; I `should_serialize_unreleased_suspended_payment_with_explicit_null_released_at` (new) | Covered |
| BR-SHR-06 | `PaymentUtils.parseTimestamp` | U `parseTimestamp_*`, `should_return_null_when_timestamp_is_date_only` (new), `should_trim_timestamp_before_parsing` (new) | R `should_return_no_suspended_payment_when_a_held_bound_is_not_in_iso_date_time_format` (new) | Covered |
| BR-SHR-07 | `PaymentUtils` | U `formatAmount_*`, `now_*`, `likePattern_*`, `randomId_*` | — (not used by production paths) | Covered |
| BR-SHR-08 | all repositories | — | R `*Search_applies*` (lower-case inputs); I `should_compute_inbound_search_age_in_days_for_every_row` (`status=received`) (new), `should_render_outbound_search_rows_in_camel_case_without_audit_timestamps` (`chargesType=sha`) (new) | Covered |
| BR-SHR-09 | `schema.sql` | — | R `should_round_amount_to_two_fractional_digits`, `should_reject_instant_payment_without_currency`, `should_reject_outbound_payment_without_amount`, `should_reject_currency_longer_than_three_characters`, `should_accept_recall_without_amount_and_currency` (all new) | Covered |
| BR-SHR-10 | entities, listeners, `RecallService` | K `should_persist_instant_event_with_invalid_iban_and_unknown_status_as_is`, `should_persist_outbound_event_with_unknown_charges_type_as_is` (new) | I `should_persist_recall_referencing_an_unknown_original_payment` (new, `R-XXX`) | Covered |

### 3.2 REST API and error contract (REST adapters)

| Rule | Implementation | Unit / slice tests | Integration tests | Status |
|---|---|---|---|---|
| BR-API-01 | controllers | W `*Search_returnsRows`, `should_return_500_with_generic_error_when_http_method_is_not_get` (new, 5 paths) | I search/details tests | Covered (+ new observation N-01, §4) |
| BR-API-02 | `GlobalExceptionHandler` | H `should_map_illegal_argument_to_400_with_its_message_as_error` (new), `should_map_translated_illegal_argument_to_400_with_its_original_message` (new); W `recallsSearch_invalidType_returns400`, `should_return_400_with_exception_message_when_service_rejects_instant_iban` (new) | I `should_return_400_with_raw_input_when_recall_type_is_unknown` (new), `should_return_400_with_message_when_instant_debtor_iban_is_invalid` (new) | Covered |
| BR-API-03 | `GlobalExceptionHandler` | H `should_map_any_other_exception_to_500_with_generic_message` (new), `should_map_invalid_data_access_api_usage_not_caused_by_illegal_argument_to_500` (new); W `instantSearch_serviceFailure_returns500`, `outboundDetails_dbFailure_returns500`, `should_return_generic_error_body_when_service_fails` (new), non-integer limit and non-GET tests (new) | — | Covered |
| BR-API-04 | controllers | W `*Details_foundAndMissing`, `outboundDetails_missing_returns404` | I `details_unknownId_returns404`, `should_return_404_with_empty_body_when_details_id_is_unknown` (new, 5 types) | Covered |
| BR-API-05 (V-PT part) | repositories `findById`, `OutboundPaymentController.get`, `GlobalExceptionHandler` | W `should_return_404_without_querying_when_outbound_details_id_is_not_a_uuid` (new) | I `should_return_400_with_invalid_uuid_message_when_details_id_is_not_a_uuid` (new, 4 types, body `{"error":"Invalid UUID string: not-a-uuid"}`), `should_return_404_with_empty_body_when_outbound_details_id_is_not_a_uuid` (new) | **Covered (fixed P2-Q1)**: 4 types 400; outbound 404 unchanged |
| BR-API-06 | repositories `id.trim()`, outbound controller | R `should_trim_details_id_before_parsing_it` (new) | I `should_trim_details_id_before_parsing_it` (new, 4 types), `should_return_404_when_outbound_details_id_is_padded_with_spaces` (new) | Covered |
| BR-API-07 (V-PT) | `GlobalExceptionHandler` | H `should_map_illegal_argument_without_message_to_400_with_default_message` (new), `should_map_translated_illegal_argument_without_message_to_400_with_default_message` (new) | I `should_return_400_with_default_message_when_a_date_filter_is_not_a_strict_iso_date` (new) | **Covered (fixed P2-Q1)**: 400 `{"error":"Bad request"}` |
| BR-API-08 (V-PT) | `GlobalExceptionHandler` | W `should_return_500_with_generic_error_when_limit_is_not_an_integer` (new, 5 paths) | — | Covered, **confirmed** |
| BR-API-09 | DTOs, outbound controller | W `should_build_outbound_details_from_entity_manager_and_never_call_the_service` (new) | I `outboundDetails_returnsPayment`, `should_render_outbound_details_as_snake_case_map_with_jackson_date_formats` (new), `should_render_outbound_search_rows_in_camel_case_without_audit_timestamps` (new), `should_serialize_unreleased_suspended_payment_with_explicit_null_released_at` (new) | Covered |

### 3.3 Event ingestion (Kafka adapters)

| Rule | Unit tests (K) | Integration tests (I) | Status |
|---|---|---|---|
| BR-ING-01 | `should_listen_to_its_own_topic_with_its_own_transactional_consumer_group` (new, 5 listeners) | `kafkaIngestion_persistsAllFivePaymentTypes` | Covered |
| BR-ING-02 | same test (group ids `jpa-*-group`) | — | Covered |
| BR-ING-03 | — | `kafkaIngestion_upsertsBySameId`, `should_fully_replace_columns_missing_from_the_later_event` (new) | Covered |
| BR-ING-04 | `should_set_created_at_and_updated_at_to_consumer_time_on_every_message` (new) | `should_overwrite_created_at_when_the_same_id_is_ingested_again` (new) | Covered |
| BR-ING-05 | `*_malformedJson_swallowed`, `should_skip_instant_event_without_id`, `should_skip_inbound_event_with_invalid_id`, `should_skip_instant_event_with_unparsable_execution_date_time`, `should_skip_outbound_event_with_unparsable_requested_execution_date`, `should_swallow_exception_raised_by_merge` (all new) | `should_skip_malformed_message_and_keep_consuming_the_topic` (new) | Covered |
| BR-ING-06 (V-PT) | `@Transactional` presence checked in the topic/group test (new) | `should_propagate_commit_time_constraint_violation_out_of_the_listener` (new, 5 listeners: `DataIntegrityViolationException`, row absent) | Covered, **confirmed** |
| BR-ING-07 (V-PT) | `should_map_missing_<type>_fields_to_empty_strings_and_json_nulls_to_null_literal` (new, 5 listeners) | `should_persist_missing_fields_as_empty_strings_and_json_nulls_as_null_literal` (new) | Covered, **confirmed** |
| BR-ING-08 | `instant_onMessage_persistsRow`, `should_parse_instant_execution_date_time_without_time_zone_conversion` (new), `should_parse_recall_initiated_at_with_same_timestamp_rules` (new), `should_parse_suspended_held_at_iso_utc_value_without_time_zone_conversion` (new) | — | Covered |
| BR-ING-09 | `should_parse_outbound_date_from_its_first_ten_characters` (new), `should_parse_inbound_and_recall_style_dates_from_their_first_ten_characters` (new) | — | Covered |
| BR-ING-10 | `should_default_scheme_to_sct_inst_when_missing_or_null` (new), `should_keep_scheme_given_by_the_event` (new), `should_ignore_api_style_execution_datetime_field_name_in_events` (new, C-06) | `kafkaIngestion_persistsAllFivePaymentTypes` | Covered |
| BR-ING-11 | `should_drop_recall_event_when_original_payment_id_is_not_a_uuid` (new: invalid, null, missing) | — | Covered |
| BR-ING-12 | `should_persist_instant_event_with_invalid_iban_and_unknown_status_as_is` (new), `should_persist_recall_with_unknown_type_without_checking_original_payment` (new), `should_persist_outbound_event_with_unknown_charges_type_as_is` (new) | `should_persist_recall_referencing_an_unknown_original_payment` (new) | Covered |

### 3.4 Instant Payments

| Rule | Unit tests | Integration tests | Status |
|---|---|---|---|
| BR-INS-01 | S `instantSearch_invalidDebtorIban_throws`, `should_ignore_blank_iban_filters_and_delegate_to_repository` (new), `should_report_debtor_iban_first_when_both_iban_filters_are_invalid` (new) | I `should_return_400_with_message_when_instant_debtor_iban_is_invalid` (new) | Covered |
| BR-INS-02 | S `instantSearch_invalidCreditorIban_throws`, `should_not_query_repository_when_creditor_iban_is_invalid` (new) | — | Covered |
| BR-INS-03 | — | R `should_match_end_to_end_id_exactly_and_case_sensitively` (new); I `kafkaIngestion_isVisibleThroughSearchApi` | Covered |
| BR-INS-04 | — | R `should_match_instant_iban_filter_case_insensitively_as_contains` (new), `should_not_strip_inner_spaces_from_instant_iban_filter` (new); I `should_search_instant_debtor_iban_as_case_insensitive_contains_without_removing_inner_spaces` (new) | Covered |
| BR-INS-05 | — | I `instantSearch_appliesStatusFilter`; R `instantSearch_appliesAllFilters_*` | Covered |
| BR-INS-06 | — | R `should_apply_inclusive_instant_execution_bounds_and_ignore_unparsable_ones` (new) | Covered |
| BR-INS-07 | — | R same test (date-only `executionTo` = end of day, unparsable ignored) | Covered |
| BR-INS-08 | — | R `instantSearch_ordersByExecutionDatetimeDescending`; I `should_return_most_recent_instant_payment_first_within_limit` (new) | Covered |
| BR-INS-09 | S `instantGetById_delegatesToRepository` | I `instantDetails_returnsPayment`, `should_return_same_payload_in_details_and_search` (new) | Covered |

### 3.5 Suspended Payments

| Rule | Unit tests | Integration tests | Status |
|---|---|---|---|
| BR-SUS-01 | — | R `should_match_suspended_debtor_iban_exactly_and_case_sensitively` (new); I `should_search_suspended_debtor_iban_exactly_without_validation` (new) | Covered |
| BR-SUS-02 | — | R `suspendedSearch_appliesParameterizedFilters`; I `suspendedSearch_appliesReviewStatusFilter` | Covered |
| BR-SUS-03 | U `parseTimestamp_*` | R `should_apply_inclusive_held_bounds` (new) | Covered |
| BR-SUS-04 (V-PT) | — | R `should_return_no_suspended_payment_when_a_held_bound_is_not_in_iso_date_time_format` (new, 5 cases); I `should_return_empty_suspended_result_when_held_bound_is_not_iso_date_time` (new) | Covered, **confirmed** |
| BR-SUS-05 | — | R `should_order_suspended_payments_by_held_at_descending_and_apply_limit` (new) | Covered |
| BR-SUS-06 | S `suspendedSearch_mapsAllFields`, `suspendedGetById_foundAndMissing`, `should_map_suspended_details_with_same_fields_as_search` (new); K `suspended_onMessage_nullReleasedAt_staysNull` | I `should_return_same_payload_in_details_and_search` (new), `should_serialize_unreleased_suspended_payment_with_explicit_null_released_at` (new) | Covered |

### 3.6 Inbound Payments

| Rule | Unit tests | Integration tests | Status |
|---|---|---|---|
| BR-INB-01 | — | R `inboundSearch_appliesFiltersAndDefaultLimit` | Covered |
| BR-INB-02 | — | R `inboundSearch_appliesFiltersAndDefaultLimit` (`bnpa`) | Covered |
| BR-INB-03 | — | I `inboundSearch_appliesStatusFilter`, `should_compute_inbound_search_age_in_days_for_every_row` (new) | Covered |
| BR-INB-04 (V-PT part) | — | R `should_reject_inbound_value_date_bound_not_in_strict_iso_date_format_with_message_less_error` (new), `should_apply_inclusive_inbound_value_date_bounds` (new); I `should_return_400_with_default_message_when_a_date_filter_is_not_a_strict_iso_date` (new) | **Covered (fixed P2-Q1)**: 400 `{"error":"Bad request"}` |
| BR-INB-05 | — | R `should_order_inbound_payments_by_value_date_then_created_at_descending` (new) | Covered |
| BR-INB-06 | — | R `should_default_inbound_limit_to_50_without_capping_at_200` (new: null→50, 201→201, 0→0); I limit -1 and 0 tests (new) | Covered |
| BR-INB-07 | S `inboundGetById_computesAgeInDays`, `should_return_negative_age_in_days_when_value_date_is_in_the_future` (new), `should_leave_age_in_days_null_when_value_date_is_null` (new); W `inboundSearch_computesAgeInDays`, `should_return_negative_age_in_days_in_search_when_value_date_is_in_the_future` (new), `should_return_null_age_in_days_in_search_when_value_date_is_null` (new) | I `should_compute_inbound_details_age_in_days_from_value_date_to_today` (new), `should_compute_inbound_search_age_in_days_for_every_row` (new) | Covered |
| BR-INB-08 | S `should_not_compute_age_in_days_in_service_search` (new); W `should_return_details_age_in_days_as_computed_by_service_without_recomputing_it` (new) | — | Covered |

### 3.7 Outbound Payments

| Rule | Unit tests | Integration tests | Status |
|---|---|---|---|
| BR-OUT-01 | — | R `outboundSearch_appliesParameterizedClauses` | Covered |
| BR-OUT-02 | — | R `outboundSearch_appliesParameterizedClauses` (`abna`), `should_not_match_lower_case_stored_destination_bic` (new) | Covered |
| BR-OUT-03 | — | I `outboundSearch_appliesChargesTypeFilter`; R `outboundSearch_appliesParameterizedClauses` | Covered |
| BR-OUT-04 (V-PT part) | — | R `should_reject_outbound_execution_bound_not_in_strict_iso_date_format_with_message_less_error` (new), `should_apply_inclusive_outbound_execution_bounds` (new); I `should_return_400_with_default_message_when_a_date_filter_is_not_a_strict_iso_date` (new) | **Covered (fixed P2-Q1)**: as BR-INB-04 |
| BR-OUT-05 | — | R `should_order_outbound_payments_by_requested_execution_date_descending_and_apply_limit` (new) | Covered |
| BR-OUT-06 | S `outboundSearch_computesFeePerChargesType`, `should_compute_search_fee_with_service_fee_table` (new, 9 cases incl. HALF_UP, unknown, null), `should_keep_search_fee_scale_of_two_for_our_payments` (new) | I `should_compute_search_fee_with_service_policy_on_seeded_outbound_payments` (new) | Covered |
| BR-OUT-07 (V-PT part) | W `outboundDetails_computesFeeFromController`, `should_compute_details_fee_with_controller_fee_table` (new, 7 cases), `should_return_500_when_details_fee_is_computed_for_our_payment_without_amount` (new), `should_return_fixed_details_fee_when_sha_payment_has_no_amount` (new) | I `should_compute_details_fee_with_controller_policy_on_seeded_outbound_payments` (new) | Covered, **confirmed** |
| BR-OUT-08 | S + W fee tables above | I seeded search fees 0.30 / 156.01 / 0 vs details 0.50 / 312.02 / 0 (new, C-01) | Covered |
| BR-OUT-09 | W `should_build_outbound_details_from_entity_manager_and_never_call_the_service` (new) | I `outboundDetails_returnsPayment`, `should_render_outbound_details_as_snake_case_map_with_jackson_date_formats` (new) | Covered (OQ-04 answered, §4) |
| BR-OUT-10 | S `outboundGetById_foundAndMissing`; W outbound details never calls the service (new) | — | Covered |

### 3.8 Recalls

| Rule | Unit tests | Integration tests | Status |
|---|---|---|---|
| BR-RCL-01 | S `recallSearch_invalidType_throws`, `should_accept_allowed_recall_types_trimmed_and_case_insensitive` (new, 6 cases), `should_reject_unknown_recall_type_with_raw_input_in_message` (new), `should_ignore_blank_recall_type_filter` (new); W `recallsSearch_invalidType_returns400` | I `recallsSearch_unknownType_returns400`, `should_return_400_with_raw_input_when_recall_type_is_unknown` (new) | Covered |
| BR-RCL-02 | — | R `recallSearch_appliesFilters` | Covered |
| BR-RCL-03 (V-PT part) | — | R `should_reject_malformed_original_payment_id_filter_with_invalid_uuid_message` (new); I `should_return_400_with_invalid_uuid_message_when_recall_original_payment_id_is_not_a_uuid` (new) | **Covered (fixed P2-Q1)**: 400 `{"error":"Invalid UUID string: xyz"}` |
| BR-RCL-04 | — | R `should_treat_date_only_recall_bounds_as_start_of_day_and_ignore_unparsable_ones` (new, C-04) | Covered |
| BR-RCL-05 | — | R `recallSearch_alwaysUsesHardcodedLimitOf50`; I `should_ignore_negative_limit_for_recalls` (new) | Covered |
| BR-RCL-06 | — | R `should_order_recalls_by_initiated_at_descending` (new) | Covered |
| BR-RCL-07 | K `should_persist_recall_with_unknown_type_without_checking_original_payment` (new, no `em.find`) | I `should_persist_recall_referencing_an_unknown_original_payment` (new) | Covered |
| BR-RCL-08 | S `recallSearch_mapsFieldsAndFormatsDates` | I `should_return_same_payload_in_details_and_search` (new) | Covered |

### 3.9 Data and environment

| Rule | Tests | Status |
|---|---|---|
| BR-DAT-01 | I `should_expose_exactly_the_five_payment_tables` (new); indirectly every Docker test | Covered |
| BR-DAT-02 | I seeded-row tests, `should_keep_three_seeded_rows_per_table_outside_the_reserved_test_range` (new) | Covered |
| BR-DAT-03 | I `@BeforeEach` cleanup; every new Kafka test uses ids `...000000000940`..`...000000000950` | Covered |

### 3.10 Conflicts C-01..C-06 (asserted as-is)

| Conflict | Pinned by |
|---|---|
| C-01 two fee policies | S `should_compute_search_fee_with_service_fee_table`; W `should_compute_details_fee_with_controller_fee_table`; I seeded search vs details fee tests |
| C-02 malformed id: outbound 404 vs others 400 | I `should_return_404_with_empty_body_when_outbound_details_id_is_not_a_uuid` vs `should_return_400_with_invalid_uuid_message_when_details_id_is_not_a_uuid` |
| C-03 limit per type | U limit policy; R inbound no cap, recall fixed 50; I negative/zero limit tests |
| C-04 date bounds per type | R instant bounds, recall bounds, suspended empty result |
| C-05 IBAN filter per type | R/I instant contains vs suspended exact without validation |
| C-06 field naming | K `should_ignore_api_style_execution_datetime_field_name_in_events`; I outbound details snake_case vs search camelCase |

## 4. Characterization findings (V-PT outcome) and the P2-Q1 fix

**What characterization found.** Every repository is a Spring `@Repository` bean, so Spring's persistence exception translation (`PersistenceExceptionTranslationPostProcessor` → `EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible`) wraps any `IllegalArgumentException` thrown **inside a repository method** into `InvalidDataAccessApiUsageException`. That is not an `IllegalArgumentException`, so the HEAD `GlobalExceptionHandler` returned **500 with the generic body**. Errors from services (IBAN check, recall type check) were not translated and already returned 400.

**User decision P2-Q1 (2026-11-12):** the analysis's 400 is the intended behaviour; fix it, with approval for the needed production change. Outbound malformed-id details stay 404. N-01 is out of scope.

**Fix (test-first).** The pinned tests were rewritten to expect 400. RED run: 13 integration failures, and the handler unit test did not compile. Then the minimal change, in `src/main/java/fr/kevin/llps/payments/controller/GlobalExceptionHandler.java` only:
- new `@ExceptionHandler(InvalidDataAccessApiUsageException.class)` `handleInvalidDataAccessApiUsage`. When the cause is an `IllegalArgumentException` → 400 with the cause message; any other cause → `handleGeneric` (500, unchanged).
- `handleBadRequest` and the new handler share a private `badRequest(message)`. A `null` message yields `{"error":"Bad request"}` (constant `DEFAULT_BAD_REQUEST_MESSAGE`) instead of the NPE thrown by `Map.of("error", null)`.

| Rule | HEAD behaviour (observed) | After the P2-Q1 fix (pinned) |
|---|---|---|
| BR-SHR-03 | `limit=-1` → 500 generic | **400** `{"error":"max-results cannot be negative"}` (instant, suspended, inbound, outbound). Recalls ignore `limit` (200). `limit=0` → 200 `[]` (answers OQ-03). |
| BR-API-05 | malformed id → 500 generic (instant, suspended, inbound, recall) | **400** `{"error":"Invalid UUID string: not-a-uuid"}`. Outbound → 404 with empty body (unchanged). |
| BR-API-07 | message-less IAE: NPE in the handler (direct) / 500 generic (repository) | **400** `{"error":"Bad request"}` in both cases. |
| BR-INB-04 / BR-OUT-04 (invalid part) | 500 generic | **400** `{"error":"Bad request"}` |
| BR-RCL-03 (invalid part) | 500 generic | **400** `{"error":"Invalid UUID string: xyz"}` |

Unchanged on purpose: BR-API-08 (non-integer `limit` → 500), N-01 (non-GET → 500), BR-API-03 (any other exception → 500).

Additional observations not in the catalog:
- **N-01** (BR-API-01/03): any non-GET request to an existing path (e.g. `POST /api/v1/*/search`) returns **500 with the generic body**, not 405, because the catch-all `@ExceptionHandler(Exception.class)` also catches `HttpRequestMethodNotSupportedException`. Pinned in W; out of P2-Q1 scope and kept as is (user clarification).
- **N-02** (OQ-04, BR-OUT-09): outbound details render `requested_execution_date` as `"2026-10-16"`, `amount` as `780.00`, and `created_at`/`updated_at` as Jackson ISO-8601 **in UTC with milliseconds** (e.g. seed local `2026-10-16 08:00:00` in `Europe/Paris` → `"2026-10-16T06:00:00.000+00:00"`). The test computes the expected value from the JVM zone, so it is deterministic in any zone.
- **N-03** (BR-INS-04): an instant IBAN filter with inner spaces passes validation but never matches stored IBANs (pattern keeps the spaces). Pinned.

## 5. Remaining gaps and regression risks

| ID | Gap / risk | Mitigation |
|---|---|---|
| G-01 | ~~§4 differences between rule text and behaviour.~~ Resolved by P2-Q1: code fixed to 400; `ddd-domain-analysis.md` rule entries, statuses, history (P2-Q1/P2-Q2) and risks updated. | — |
| R-11 (new) | The 400 responses rely on Spring translating repository `IllegalArgumentException`s and on the new `GlobalExceptionHandler.handleInvalidDataAccessApiUsage`. Moving parsing into the domain/application layer, or into adapters that throw other exception types, must keep the same status **and** body. Side effect of the fix: any other repository `IllegalArgumentException` (e.g. an invalid JPQL string) now surfaces as 400 instead of 500; none exists today (queries are static and covered). | Pinned by H unit tests and I tests on the full stack; Phase 5 must keep them green. |
| R-03 | `ageInDays` depends on the current date. | Tests compute expectations from `LocalDate.now()`; a run crossing midnight could fail once. A clock port in Phase 3/5 removes this. |
| R-02 | Time-zone dependence (Jackson UTC vs `SimpleDateFormat` local). | N-02 test derives the expected value from the JVM zone; other format tests use zone-independent local values. |
| R-06 | `data.sql` is destructive; seeds are fixtures. | Seed-count test added; new Kafka ids in reserved `...09` range. |
| R-08 | Accidental unification of quirks. | C-01..C-06 pinned (§3.10). |
| G-02 | Kafka container error handling after a commit failure (`DefaultErrorHandler` retries, then skips) is not asserted; only the propagation out of the listener is pinned (BR-ING-06). | Acceptable: retry policy is a framework default, not a documented rule. |
| G-03 | `PaymentUtils.FMT` thread-safety (R-01) is not testable deterministically. | Format output pinned; replacement in Phase 5 must keep outputs. |

No rule is **Blocked** or **Not covered**.

## 6. Test execution strategy

| Tier | Command (from the module directory) | Docker | Tests (test-method time reported by Surefire) | When |
|---|---|---|---|---|
| Fast | `mvn test -Dtest='PaymentServiceUnitTest,KafkaListenerUnitTest,PaymentControllerWebTest,PaymentUtilsTest,GlobalExceptionHandlerTest'` | no | 163 (< 2 s) | every TDD step |
| Repository slice | `mvn test -Dtest=PaymentRepositoryUnitTest` | yes | 58 (~5 s, plus container start) | after any persistence change |
| Integration | `mvn test -Dtest=PaymentApplicationIntegrationTest` | yes | 86 (~6 s, plus context, container and embedded Kafka start) | after any adapter / wiring change |
| Full | `mvn test` | yes | 307 | before each Phase 5 commit gate and at Phase 6 |

Note: the module `AGENTS.md` fast-tier command does not list the new `GlobalExceptionHandlerTest`; it still runs under `mvn test`.

Execution evidence (2026-11-12, this phase):

| Run | Command | Result |
|---|---|---|
| Baseline | `mvn test` | exit 0, 83 tests, 0 failures |
| Fast tier | command above | exit 0, 160 tests (28 U, 40 S, 49 K, 40 W, 3 H), 0 failures |
| Docker tier | `mvn test -Dtest='PaymentRepositoryUnitTest,PaymentApplicationIntegrationTest'` | exit 0, 144 tests (58 R, 86 I), 0 failures |
| Full (characterization, before P2-Q1) | `mvn test` | exit 0, 304 tests, 0 failures |
| RED (P2-Q1, tests rewritten to 400) | `mvn test -Dtest=GlobalExceptionHandlerTest` / `-Dtest=PaymentApplicationIntegrationTest` | exit 1: handler test does not compile (missing `handleInvalidDataAccessApiUsage`); IT 86 run, **13 failures** (all on the expected 400 status) |
| GREEN (after the fix) | `mvn test -Dtest='GlobalExceptionHandlerTest,PaymentApplicationIntegrationTest'` | 92 run: H 6/6 green; IT status assertions green, 4 failures on a placeholder body used to read Hibernate's message, then pinned as `max-results cannot be negative` |
| Full (final) | `mvn test` | exit 0, **307 tests** (U 28, S 40, K 49, W 40, H 6, R 58, I 86), 0 failures, 0 errors, 0 skipped; BUILD SUCCESS |

The integration suite ran green in three full-stack runs (Docker tier, full before P2-Q1, full after P2-Q1) with no flaky behaviour.

## 7. Measurable acceptance criteria for migration stages

| ID | Criterion | Measure |
|---|---|---|
| MAC-01 | No behaviour regression | `mvn test` exit 0 with **all 307 Phase 2 tests** (or their migrated equivalents) green after every Phase 5 iteration. |
| MAC-02 | No silent test loss | Test count never drops below 307 unless the user approves a mapping old test → new test; every rule in §3 keeps at least one unit-level and, where listed, one integration-level test. |
| MAC-03 | Conflicts preserved | All C-01..C-06 tests in §3.10 stay green, unchanged in their expected values. |
| MAC-04 | Error contract preserved | The H and I tests asserting exact status + body (400 with message or `Bad request` / 404 / 500 generic) stay green per endpoint, including the P2-Q1 400 responses (§4). |
| MAC-05 | Ingestion semantics preserved | BR-ING-03..12 tests stay green, including commit-time propagation (BR-ING-06) and the `"null"` / `""` mapping (BR-ING-07). |
| MAC-06 | Seeds and schema untouched | BR-DAT-01..03 tests green; `schema.sql` / `data.sql` diff empty unless approved. |
| MAC-07 | Test relocation by context | When code moves to per-context modules/packages, each test moves to the owning context (§3 table) without changing its assertions; tests that move between tiers (e.g. listener unit → adapter unit) keep the same inputs and expected values. |
| MAC-08 | Domain tests stay infrastructure-free | New domain tests (Phase 5) run without Docker, Spring context, or Kafka. |

## 8. Files changed (uncommitted)

- `src/main/java/fr/kevin/llps/payments/controller/GlobalExceptionHandler.java` (production, approved by P2-Q1: +22 / -1 lines)

- `src/test/java/fr/kevin/llps/payments/util/PaymentUtilsTest.java` (+11 tests)
- `src/test/java/fr/kevin/llps/payments/service/PaymentServiceUnitTest.java` (+26 tests)
- `src/test/java/fr/kevin/llps/payments/kafka/KafkaListenerUnitTest.java` (+39 tests)
- `src/test/java/fr/kevin/llps/payments/controller/PaymentControllerWebTest.java` (+26 tests)
- `src/test/java/fr/kevin/llps/payments/controller/GlobalExceptionHandlerTest.java` (new, 6 tests)
- `src/test/java/fr/kevin/llps/payments/repository/PaymentRepositoryUnitTest.java` (+46 tests)
- `src/test/java/fr/kevin/llps/payments/integration/PaymentApplicationIntegrationTest.java` (+70 tests)
- `docs/migration-2026-11-12/ddd-test-coverage.md` (this report)
- `docs/migration-2026-11-12/ddd-domain-analysis.md` (affected rule entries, statuses, §6 rows, §16 history, §17 risks, §18 counts)

Organization decision: new tests were added to the existing tier classes (sections labelled per bounded context and rule IDs), rather than new per-context classes, so the costly Spring/Testcontainers contexts are not duplicated. New tests follow `docs/java-coding-conventions.md` (snake_case names, GIVEN/WHEN/THEN, `@MockitoBean`, Awaitility); existing legacy-style test names were left untouched. A temporary probe test used to observe the real behaviour before writing assertions was deleted and is not part of the change set.

## 9. Validation gate decisions (2026-11-12, relayed by the orchestrator)

| ID | Question | Decision |
|---|---|---|
| P2-Q1 | Treatment of the §4 differences | (b) Fix to 400: the analysis's 400 is intended; production change approved; outbound malformed-id details stay 404; N-01 out of scope (kept 500). |
| P2-Q2 | Test organization | (a) Keep tests in the existing classes, in per-bounded-context sections. |
| P2-Q3 | Commit | (c) Do not commit yet; continue with changes uncommitted. |
