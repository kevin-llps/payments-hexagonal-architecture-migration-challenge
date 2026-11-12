package fr.kevin.llps.payments.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InboundPaymentSearchRequest;
import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentSearchRequest;
import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.dto.RecallSearchRequest;
import fr.kevin.llps.payments.entity.InboundPayment;
import fr.kevin.llps.payments.entity.InstantPayment;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.entity.Recall;
import fr.kevin.llps.payments.entity.SuspendedPayment;

import jakarta.persistence.PersistenceException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({InstantPaymentRepository.class, InboundPaymentRepository.class,
        OutboundPaymentRepository.class, RecallRepository.class})
@Testcontainers
class PaymentRepositoryUnitTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private InstantPaymentRepository instantRepository;
    @Autowired
    private InboundPaymentRepository inboundRepository;
    @Autowired
    private OutboundPaymentRepository outboundRepository;
    @Autowired
    private RecallRepository recallRepository;
    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void clearTables() {
        em.getEntityManager().createNativeQuery("DELETE FROM instant_payments").executeUpdate();
        em.getEntityManager().createNativeQuery("DELETE FROM suspended_payments").executeUpdate();
        em.getEntityManager().createNativeQuery("DELETE FROM inbound_payments").executeUpdate();
        em.getEntityManager().createNativeQuery("DELETE FROM outbound_payments").executeUpdate();
        em.getEntityManager().createNativeQuery("DELETE FROM recalls").executeUpdate();
        em.flush();
        em.clear();
    }

    // ---------------- instant ----------------

    @Test
    void instantSearch_appliesAllFilters_andFormatsExecutionDatetime() {
        InstantPayment match = instant("00000000-0000-0000-0000-000000000601", "E2E-T1", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR");
        InstantPayment other = instant("00000000-0000-0000-0000-000000000602", "E2E-T2", "PENDING",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 10:00:00"), "EUR");
        persist(match, other);

        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.endToEndId = "E2E-T1";
        request.debtorIban = "de89";
        request.creditorIban = "fr76";
        request.status = "completed";
        request.currency = "eur";
        request.executionFrom = "2026-10-01T00:00:00";
        request.executionTo = "2026-10-31";
        request.limit = 10;

        List<InstantPaymentResponse> results = instantRepository.search(request);

        assertThat(results).hasSize(1);
        InstantPaymentResponse response = results.get(0);
        assertThat(response.id).isEqualTo("00000000-0000-0000-0000-000000000601");
        assertThat(response.endToEndId).isEqualTo("E2E-T1");
        assertThat(response.amount).isEqualByComparingTo("1500.00");
        assertThat(response.executionDatetime).isEqualTo("2026-10-15T09:15:00");
        assertThat(response.scheme).isEqualTo("SCT INST");
    }

    @Test
    void instantSearch_ordersByExecutionDatetimeDescending() {
        persist(
                instant("00000000-0000-0000-0000-000000000603", "E2E-OLD", "PENDING",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-10 08:00:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000604", "E2E-NEW", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 08:00:00"), "EUR"));

        List<InstantPaymentResponse> results = instantRepository.search(new InstantPaymentSearchRequest());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).endToEndId).isEqualTo("E2E-NEW");
        assertThat(results.get(1).endToEndId).isEqualTo("E2E-OLD");
    }

    @Test
    void instantFindById_foundAndMissing() {
        persist(instant("00000000-0000-0000-0000-000000000605", "E2E-F1", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));

        InstantPaymentResponse found = instantRepository.findById("00000000-0000-0000-0000-000000000605");
        assertThat(found.endToEndId).isEqualTo("E2E-F1");
        assertThat(found.executionDatetime).isEqualTo("2026-10-15T09:15:00");
        assertThat(instantRepository.findById("11111111-1111-1111-1111-111111111111")).isNull();
    }

    // ---------------- inbound ----------------

    @Test
    void inboundSearch_appliesFiltersAndDefaultLimit() {
        persist(
                inbound("00000000-0000-0000-0000-000000000621", "CTR-1", "DE89370400440532013000", "BNPAFRPP",
                        "SETTLED", "EUR", Date.valueOf("2026-10-15"), Date.valueOf("2026-10-15")),
                inbound("00000000-0000-0000-0000-000000000622", "CTR-2", "DE89370400440532013000", "ABNANL2A",
                        "RECEIVED", "EUR", Date.valueOf("2026-10-16"), null),
                inbound("00000000-0000-0000-0000-000000000623", "CTR-3", "DE89370400440532013000", "BNPAFRPP",
                        "SETTLED", "EUR", Date.valueOf("2026-09-01"), Date.valueOf("2026-09-01")));

        InboundPaymentSearchRequest request = new InboundPaymentSearchRequest();
        request.creditorIban = "DE89370400440532013000";
        request.originBic = "bnpa";
        request.status = "settled";
        request.currency = "eur";
        request.valueDateFrom = "2026-10-01";
        request.valueDateTo = "2026-10-31";

        List<InboundPaymentResponse> results = inboundRepository.search(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).creditTransferRef).isEqualTo("CTR-1");
        assertThat(results.get(0).valueDate).isEqualTo("2026-10-15");
        assertThat(results.get(0).settlementDate).isEqualTo("2026-10-15");
    }

    @Test
    void inboundFindById_foundAndMissing() {
        persist(inbound("00000000-0000-0000-0000-000000000624", "CTR-4", "DE89370400440532013000", "BNPAFRPP",
                "RECEIVED", "EUR", Date.valueOf("2026-10-17"), null));

        InboundPaymentResponse found = inboundRepository.findById("00000000-0000-0000-0000-000000000624");
        assertThat(found.creditTransferRef).isEqualTo("CTR-4");
        assertThat(found.valueDate).isEqualTo("2026-10-17");
        assertThat(found.settlementDate).isNull();
        assertThat(inboundRepository.findById("11111111-1111-1111-1111-111111111111")).isNull();
    }

    // ---------------- outbound ----------------

    @Test
    void outboundSearch_appliesParameterizedClauses() {
        persist(
                outbound("00000000-0000-0000-0000-000000000631", "OUT-1", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000632", "OUT-2", "DE89370400440532013000", "BNPAFRPP",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000633", "OUT-3", "DE89370400440532013000", "ABNANL2A",
                        "OUR", "SENT", Date.valueOf("2026-10-16")));

        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.debtorIban = "DE89370400440532013000";
        request.destinationBic = "abna";
        request.chargesType = "sha";
        request.status = "sent";
        request.executionFrom = "2026-10-01";
        request.executionTo = "2026-10-31";
        request.limit = 20;

        List<OutboundPayment> results = outboundRepository.search(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).instructionId).isEqualTo("OUT-1");
        assertThat(results.get(0).requestedExecutionDate).isEqualTo(Date.valueOf("2026-10-16"));
    }

    @Test
    void outboundFindById_foundAndMissing() {
        persist(outbound("00000000-0000-0000-0000-000000000634", "OUT-4", "DE89370400440532013000", "ABNANL2A",
                "SHA", "SENT", Date.valueOf("2026-10-18")));

        OutboundPayment found = outboundRepository.findById("00000000-0000-0000-0000-000000000634");
        assertThat(found.instructionId).isEqualTo("OUT-4");
        assertThat(outboundRepository.findById("11111111-1111-1111-1111-111111111111")).isNull();
    }

    // ---------------- recalls ----------------

    @Test
    void recallSearch_alwaysUsesHardcodedLimitOf50() {
        for (int i = 0; i < 51; i++) {
            persist(recall(String.format("00000000-0000-0000-0000-00000000%04d", 700 + i),
                    "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"), Date.valueOf("2026-10-18")));
        }

        RecallSearchRequest request = new RecallSearchRequest();
        request.limit = 5;

        assertThat(recallRepository.search(request)).hasSize(50);
    }

    @Test
    void recallSearch_appliesFilters() {
        persist(
                recall("00000000-0000-0000-0000-000000000751", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")),
                recall("00000000-0000-0000-0000-000000000752", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")));

        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = "r-cor";
        request.originalPaymentId = "00000000-0000-0000-0000-000000000401";
        request.originalPaymentType = "outbound";
        request.reasonCode = "ac04";
        request.status = "initiated";
        request.initiatedFrom = "2026-10-01T00:00:00";
        request.initiatedTo = "2026-10-31T00:00:00";

        List<Recall> results = recallRepository.search(request);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).recallReference).isEqualTo("RCL-T1");
    }

    @Test
    void recallFindById_foundAndMissing() {
        persist(recall("00000000-0000-0000-0000-000000000753", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18")));

        Recall found = recallRepository.findById("00000000-0000-0000-0000-000000000753");
        assertThat(found.recallType).isEqualTo("R-COR");
        assertThat(found.deadline).isEqualTo(Date.valueOf("2026-10-18"));
        assertThat(recallRepository.findById("11111111-1111-1111-1111-111111111111")).isNull();
    }

    // ---------------- characterization: Instant date bounds (BR-INS-06, BR-INS-07, C-04) ----------------

    @ParameterizedTest(name = "[{index}] executionFrom={0} executionTo={1} -> {2} rows")
    @CsvSource(value = {
            "garbage, NULL, 2",
            "NULL, garbage, 2",
            "2026-10-16, NULL, 1",
            "NULL, 2026-10-15, 1",
            "2026-10-15T09:15:00, 2026-10-15T09:15:00, 1"
    }, nullValues = "NULL")
    void should_apply_inclusive_instant_execution_bounds_and_ignore_unparsable_ones(String from, String to,
            int expectedCount) {
        // GIVEN
        persist(
                instant("00000000-0000-0000-0000-000000000641", "E2E-B1", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000642", "E2E-B2", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 00:00:00"), "EUR"));
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.executionFrom = from;
        request.executionTo = to;

        // WHEN
        List<InstantPaymentResponse> results = instantRepository.search(request);

        // THEN
        assertThat(results).hasSize(expectedCount);
    }

    @Test
    void should_match_instant_iban_filter_case_insensitively_as_contains() {
        // GIVEN
        persist(instant("00000000-0000-0000-0000-000000000643", "E2E-C1", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.debtorIban = " 0532013 ";

        // WHEN
        List<InstantPaymentResponse> results = instantRepository.search(request);

        // THEN
        assertThat(results).extracting(r -> r.endToEndId).containsExactly("E2E-C1");
    }

    @Test
    void should_not_strip_inner_spaces_from_instant_iban_filter() {
        // GIVEN
        persist(instant("00000000-0000-0000-0000-000000000644", "E2E-C2", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.debtorIban = "DE89 3704 0044 0532 0130 00";

        // WHEN
        List<InstantPaymentResponse> results = instantRepository.search(request);

        // THEN
        assertThat(results).isEmpty();
    }

    @Test
    void should_match_end_to_end_id_exactly_and_case_sensitively() {
        // GIVEN
        persist(instant("00000000-0000-0000-0000-000000000645", "E2E-Case", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.endToEndId = "e2e-case";

        // WHEN
        List<InstantPaymentResponse> results = instantRepository.search(request);

        // THEN
        assertThat(results).isEmpty();
    }

    @Test
    void should_return_no_instant_payment_when_limit_is_zero() {
        // GIVEN
        persist(instant("00000000-0000-0000-0000-000000000646", "E2E-L0", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.limit = 0;

        // WHEN
        List<InstantPaymentResponse> results = instantRepository.search(request);

        // THEN
        assertThat(results).isEmpty();
    }

    @Test
    void should_reject_negative_instant_limit_with_illegal_argument_root_cause() {
        // GIVEN
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.limit = -1;

        // WHEN / THEN
        assertThatThrownBy(() -> instantRepository.search(request))
                .satisfies(e -> assertThat(rootCauseOf(e)).isInstanceOf(IllegalArgumentException.class));
    }

    // ---------------- characterization: Inbound order, limit and dates (BR-INB-04, BR-INB-05, BR-INB-06) ----------------

    @Test
    void should_order_inbound_payments_by_value_date_then_created_at_descending() {
        // GIVEN
        InboundPayment olderSameDay = inbound("00000000-0000-0000-0000-000000000661", "CTR-A",
                "DE89370400440532013000", "BNPAFRPP", "SETTLED", "EUR", Date.valueOf("2026-10-15"), null);
        InboundPayment newerSameDay = inbound("00000000-0000-0000-0000-000000000662", "CTR-B",
                "DE89370400440532013000", "BNPAFRPP", "SETTLED", "EUR", Date.valueOf("2026-10-15"), null);
        newerSameDay.createdAt = Timestamp.valueOf("2026-10-15 18:00:00");
        InboundPayment latestValueDate = inbound("00000000-0000-0000-0000-000000000663", "CTR-C",
                "DE89370400440532013000", "BNPAFRPP", "SETTLED", "EUR", Date.valueOf("2026-10-16"), null);
        persist(olderSameDay, newerSameDay, latestValueDate);

        // WHEN
        List<InboundPaymentResponse> results = inboundRepository.search(new InboundPaymentSearchRequest());

        // THEN
        assertThat(results).extracting(r -> r.creditTransferRef).containsExactly("CTR-C", "CTR-B", "CTR-A");
    }

    @ParameterizedTest(name = "[{index}] limit {0} -> {1} rows")
    @CsvSource(value = {
            "NULL, 50",
            "201, 201",
            "0, 0"
    }, nullValues = "NULL")
    void should_default_inbound_limit_to_50_without_capping_at_200(Integer limit, int expectedCount) {
        // GIVEN
        for (int i = 0; i < 201; i++) {
            em.persist(inbound(String.format("00000000-0000-0000-0000-0000000%05d", 10000 + i), "CTR-" + i,
                    "DE89370400440532013000", "BNPAFRPP", "SETTLED", "EUR", Date.valueOf("2026-10-15"), null));
        }
        em.flush();
        em.clear();
        InboundPaymentSearchRequest request = new InboundPaymentSearchRequest();
        request.limit = limit;

        // WHEN
        List<InboundPaymentResponse> results = inboundRepository.search(request);

        // THEN
        assertThat(results).hasSize(expectedCount);
    }

    @ParameterizedTest(name = "[{index}] valueDateFrom={0} valueDateTo={1}")
    @CsvSource(value = {
            "2026-13-45, NULL",
            "NULL, garbage",
            "2026-10-15T00:00:00, NULL"
    }, nullValues = "NULL")
    void should_reject_inbound_value_date_bound_not_in_strict_iso_date_format_with_message_less_error(String from,
            String to) {
        // GIVEN
        InboundPaymentSearchRequest request = new InboundPaymentSearchRequest();
        request.valueDateFrom = from;
        request.valueDateTo = to;

        // WHEN / THEN
        assertThatThrownBy(() -> inboundRepository.search(request))
                .satisfies(e -> assertThat(rootCauseOf(e)).isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessage(null));
    }

    @Test
    void should_apply_inclusive_inbound_value_date_bounds() {
        // GIVEN
        persist(inbound("00000000-0000-0000-0000-000000000664", "CTR-D", "DE89370400440532013000", "BNPAFRPP",
                "SETTLED", "EUR", Date.valueOf("2026-10-15"), null));
        InboundPaymentSearchRequest request = new InboundPaymentSearchRequest();
        request.valueDateFrom = "2026-10-15";
        request.valueDateTo = " 2026-10-15 ";

        // WHEN
        List<InboundPaymentResponse> results = inboundRepository.search(request);

        // THEN
        assertThat(results).extracting(r -> r.creditTransferRef).containsExactly("CTR-D");
    }

    // ---------------- characterization: Outbound filters, order, limit and dates (BR-OUT-02, BR-OUT-04, BR-OUT-05) ----------------

    @Test
    void should_not_match_lower_case_stored_destination_bic() {
        // GIVEN
        persist(outbound("00000000-0000-0000-0000-000000000671", "OUT-LC", "DE89370400440532013000", "abnanl2a",
                "SHA", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.destinationBic = "abna";

        // WHEN
        List<OutboundPayment> results = outboundRepository.search(request);

        // THEN
        assertThat(results).isEmpty();
    }

    @Test
    void should_order_outbound_payments_by_requested_execution_date_descending_and_apply_limit() {
        // GIVEN
        persist(
                outbound("00000000-0000-0000-0000-000000000672", "OUT-OLD", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-14")),
                outbound("00000000-0000-0000-0000-000000000673", "OUT-NEW", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-18")),
                outbound("00000000-0000-0000-0000-000000000674", "OUT-MID", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.limit = 2;

        // WHEN
        List<OutboundPayment> results = outboundRepository.search(request);

        // THEN
        assertThat(results).extracting(p -> p.instructionId).containsExactly("OUT-NEW", "OUT-MID");
    }

    @ParameterizedTest(name = "[{index}] executionFrom={0} executionTo={1}")
    @CsvSource(value = {
            "garbage, NULL",
            "NULL, 2026-02-30x",
            "2026-10-16T00:00:00, NULL"
    }, nullValues = "NULL")
    void should_reject_outbound_execution_bound_not_in_strict_iso_date_format_with_message_less_error(String from,
            String to) {
        // GIVEN
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.executionFrom = from;
        request.executionTo = to;

        // WHEN / THEN
        assertThatThrownBy(() -> outboundRepository.search(request))
                .satisfies(e -> assertThat(rootCauseOf(e)).isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessage(null));
    }

    @Test
    void should_apply_inclusive_outbound_execution_bounds() {
        // GIVEN
        persist(outbound("00000000-0000-0000-0000-000000000675", "OUT-EQ", "DE89370400440532013000", "ABNANL2A",
                "SHA", "SENT", Date.valueOf("2026-10-16")));
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.executionFrom = "2026-10-16";
        request.executionTo = "2026-10-16";

        // WHEN
        List<OutboundPayment> results = outboundRepository.search(request);

        // THEN
        assertThat(results).extracting(p -> p.instructionId).containsExactly("OUT-EQ");
    }

    // ---------------- characterization: Recalls (BR-RCL-03, BR-RCL-04, BR-RCL-06) ----------------

    @Test
    void should_reject_malformed_original_payment_id_filter_with_invalid_uuid_message() {
        // GIVEN
        RecallSearchRequest request = new RecallSearchRequest();
        request.originalPaymentId = "xyz";

        // WHEN / THEN
        assertThatThrownBy(() -> recallRepository.search(request))
                .satisfies(e -> assertThat(rootCauseOf(e)).isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Invalid UUID string: xyz"));
    }

    @ParameterizedTest(name = "[{index}] initiatedFrom={0} initiatedTo={1} -> {2} rows")
    @CsvSource(value = {
            "NULL, 2026-10-16, 1",
            "NULL, 2026-10-17, 2",
            "2026-10-16, NULL, 1",
            "garbage, NULL, 2",
            "NULL, garbage, 2",
            "2026-10-16T12:00:00, 2026-10-16T12:00:00, 1"
    }, nullValues = "NULL")
    void should_treat_date_only_recall_bounds_as_start_of_day_and_ignore_unparsable_ones(String from, String to,
            int expectedCount) {
        // GIVEN
        persist(
                recall("00000000-0000-0000-0000-000000000761", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")),
                recall("00000000-0000-0000-0000-000000000762", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")));
        RecallSearchRequest request = new RecallSearchRequest();
        request.initiatedFrom = from;
        request.initiatedTo = to;

        // WHEN
        List<Recall> results = recallRepository.search(request);

        // THEN
        assertThat(results).hasSize(expectedCount);
    }

    @Test
    void should_order_recalls_by_initiated_at_descending() {
        // GIVEN
        persist(
                recall("00000000-0000-0000-0000-000000000763", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")),
                recall("00000000-0000-0000-0000-000000000764", "R-UPD", Timestamp.valueOf("2026-10-16 20:00:00"),
                        Date.valueOf("2026-10-19")),
                recall("00000000-0000-0000-0000-000000000765", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")));

        // WHEN
        List<Recall> results = recallRepository.search(new RecallSearchRequest());

        // THEN
        assertThat(results).extracting(r -> r.recallType).containsExactly("R-UPD", "R-COR", "R-REJ");
    }

    // ---------------- characterization: schema constraints (BR-SHR-09, BR-DAT-01) ----------------

    @Test
    void should_round_amount_to_two_fractional_digits() {
        // GIVEN
        InstantPayment payment = instant("00000000-0000-0000-0000-000000000681", "E2E-R1", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR");
        payment.amount = new BigDecimal("10.005");
        persist(payment);

        // WHEN
        InstantPaymentResponse found = instantRepository.findById("00000000-0000-0000-0000-000000000681");

        // THEN
        assertThat(found.amount).isEqualTo(new BigDecimal("10.01"));
    }

    @Test
    void should_reject_instant_payment_without_currency() {
        // GIVEN
        InstantPayment payment = instant("00000000-0000-0000-0000-000000000682", "E2E-R2", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), null);

        // WHEN / THEN
        assertThatThrownBy(() -> persist(payment))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("not-null");
    }

    @Test
    void should_reject_outbound_payment_without_amount() {
        // GIVEN
        OutboundPayment payment = outbound("00000000-0000-0000-0000-000000000683", "OUT-NA", "DE89370400440532013000",
                "ABNANL2A", "SHA", "SENT", Date.valueOf("2026-10-16"));
        payment.amount = null;

        // WHEN / THEN
        assertThatThrownBy(() -> persist(payment))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("not-null");
    }

    @Test
    void should_reject_currency_longer_than_three_characters() {
        // GIVEN
        SuspendedPayment payment = suspended("00000000-0000-0000-0000-000000000684", "DE89370400440532013000",
                "AML_THRESHOLD", "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null);
        payment.currency = "EURO";

        // WHEN / THEN
        assertThatThrownBy(() -> persist(payment))
                .isInstanceOf(PersistenceException.class)
                .rootCause().hasMessageContaining("value too long");
    }

    @Test
    void should_accept_recall_without_amount_and_currency() {
        // GIVEN
        Recall recall = recall("00000000-0000-0000-0000-000000000685", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18"));
        recall.amount = null;
        recall.currency = null;
        persist(recall);

        // WHEN
        Recall found = recallRepository.findById("00000000-0000-0000-0000-000000000685");

        // THEN
        assertThat(found.amount).isNull();
        assertThat(found.currency).isNull();
    }

    @Test
    void should_trim_details_id_before_parsing_it() {
        // GIVEN
        persist(inbound("00000000-0000-0000-0000-000000000686", "CTR-TRIM", "DE89370400440532013000", "BNPAFRPP",
                "SETTLED", "EUR", Date.valueOf("2026-10-15"), null));

        // WHEN
        InboundPaymentResponse found = inboundRepository.findById("  00000000-0000-0000-0000-000000000686 ");

        // THEN
        assertThat(found.creditTransferRef).isEqualTo("CTR-TRIM");
    }

    // ---------------- helpers ----------------

    private static Throwable rootCauseOf(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private void persist(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    private InstantPayment instant(String id, String endToEndId, String status, String debtorIban,
            String creditorIban, Timestamp executionDatetime, String currency) {
        InstantPayment payment = new InstantPayment();
        payment.id = UUID.fromString(id);
        payment.endToEndId = endToEndId;
        payment.uetr = "uetr-" + endToEndId;
        payment.amount = new BigDecimal("1500.00");
        payment.currency = currency;
        payment.debtorIban = debtorIban;
        payment.debtorName = "ACME";
        payment.creditorIban = creditorIban;
        payment.creditorName = "Dupont";
        payment.executionDatetime = executionDatetime;
        payment.status = status;
        payment.scheme = "SCT INST";
        payment.createdAt = executionDatetime;
        payment.updatedAt = executionDatetime;
        return payment;
    }

    private SuspendedPayment suspended(String id, String debtorIban, String holdReason, String reviewStatus,
            Timestamp heldAt, Timestamp releasedAt) {
        SuspendedPayment payment = new SuspendedPayment();
        payment.id = UUID.fromString(id);
        payment.amount = new BigDecimal("12000.00");
        payment.currency = "EUR";
        payment.debtorIban = debtorIban;
        payment.debtorName = "ACME";
        payment.creditorIban = "FR7630006000011234567890189";
        payment.creditorName = "Dupont";
        payment.holdReason = holdReason;
        payment.complianceCaseRef = "CASE-" + id.substring(id.length() - 3);
        payment.reviewStatus = reviewStatus;
        payment.heldAt = heldAt;
        payment.releasedAt = releasedAt;
        payment.createdAt = heldAt;
        payment.updatedAt = heldAt;
        return payment;
    }

    private InboundPayment inbound(String id, String creditTransferRef, String creditorIban, String originBic,
            String status, String currency, Date valueDate, Date settlementDate) {
        InboundPayment payment = new InboundPayment();
        payment.id = UUID.fromString(id);
        payment.creditTransferRef = creditTransferRef;
        payment.amount = new BigDecimal("3400.00");
        payment.currency = currency;
        payment.creditorIban = creditorIban;
        payment.creditorName = "ACME";
        payment.originBic = originBic;
        payment.valueDate = valueDate;
        payment.settlementDate = settlementDate;
        payment.status = status;
        payment.createdAt = Timestamp.valueOf(valueDate.toString() + " 07:45:00");
        payment.updatedAt = payment.createdAt;
        return payment;
    }

    private OutboundPayment outbound(String id, String instructionId, String debtorIban, String destinationBic,
            String chargesType, String status, Date requestedExecutionDate) {
        OutboundPayment payment = new OutboundPayment();
        payment.id = UUID.fromString(id);
        payment.instructionId = instructionId;
        payment.amount = new BigDecimal("780.00");
        payment.currency = "EUR";
        payment.debtorIban = debtorIban;
        payment.debtorName = "ACME";
        payment.destinationBic = destinationBic;
        payment.requestedExecutionDate = requestedExecutionDate;
        payment.chargesType = chargesType;
        payment.status = status;
        payment.createdAt = Timestamp.valueOf(requestedExecutionDate.toString() + " 08:00:00");
        payment.updatedAt = payment.createdAt;
        return payment;
    }

    private Recall recall(String id, String recallType, Timestamp initiatedAt, Date deadline) {
        Recall recall = new Recall();
        recall.id = UUID.fromString(id);
        recall.recallReference = "RCL-T1";
        recall.recallType = recallType;
        recall.originalPaymentId = UUID.fromString("00000000-0000-0000-0000-000000000401");
        recall.originalPaymentType = "OUTBOUND";
        recall.reasonCode = "AC04";
        recall.amount = new BigDecimal("300.00");
        recall.currency = "EUR";
        recall.initiatedAt = initiatedAt;
        recall.deadline = deadline;
        recall.status = "INITIATED";
        recall.createdAt = initiatedAt;
        recall.updatedAt = initiatedAt;
        return recall;
    }
}
