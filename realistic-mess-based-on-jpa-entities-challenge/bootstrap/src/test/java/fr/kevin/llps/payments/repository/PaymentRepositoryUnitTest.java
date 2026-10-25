package fr.kevin.llps.payments.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import fr.kevin.llps.payments.entity.InboundPayment;
import fr.kevin.llps.payments.entity.InstantPayment;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.compliancehold.SuspendedPaymentSearchCriteria;
import fr.kevin.llps.payments.compliancehold.adapter.out.persistence.JpaSuspendedPaymentAdapter;
import fr.kevin.llps.payments.compliancehold.adapter.out.persistence.entity.SuspendedPayment;
import fr.kevin.llps.payments.recall.adapter.out.persistence.JpaRecallAdapter;
import fr.kevin.llps.payments.recall.adapter.out.persistence.entity.Recall;
import fr.kevin.llps.payments.recall.RecallSearchCriteria;
import fr.kevin.llps.payments.shared.domain.PaymentId;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({InstantPaymentRepository.class, JpaSuspendedPaymentAdapter.class, InboundPaymentRepository.class,
        OutboundPaymentRepository.class, JpaRecallAdapter.class})
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
    private JpaSuspendedPaymentAdapter suspendedRepository;
    @Autowired
    private InboundPaymentRepository inboundRepository;
    @Autowired
    private OutboundPaymentRepository outboundRepository;
    @Autowired
    private JpaRecallAdapter recallRepository;
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
    void instantSearch_ibanFilters_matchAnySubstringIgnoringCase() {
        persist(instant("00000000-0000-0000-0000-000000000606", "E2E-S1", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));

        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.debtorIban = " 0532013 ";
        request.creditorIban = "6000011234";

        assertThat(instantRepository.search(request)).extracting(r -> r.endToEndId).containsExactly("E2E-S1");
    }

    @Test
    void instantSearch_dateOnlyBounds_coverTheWholeDay() {
        persist(
                instant("00000000-0000-0000-0000-000000000607", "E2E-MIDNIGHT", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 00:00:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000608", "E2E-LATE", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 23:30:00"), "EUR"),
                instant("00000000-0000-0000-0000-000000000609", "E2E-NEXT", "COMPLETED",
                        "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-16 00:00:00"), "EUR"));

        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.executionFrom = "2026-10-15";
        request.executionTo = "2026-10-15";

        assertThat(instantRepository.search(request)).extracting(r -> r.endToEndId)
                .containsExactly("E2E-LATE", "E2E-MIDNIGHT");
    }

    @Test
    void instantSearch_unparseableExecutionBounds_areIgnored() {
        persist(instant("00000000-0000-0000-0000-000000000610", "E2E-ANY", "COMPLETED",
                "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));

        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.executionFrom = "yesterday";
        request.executionTo = "2026/10/01";

        assertThat(instantRepository.search(request)).extracting(r -> r.endToEndId).containsExactly("E2E-ANY");
    }

    @Test
    void instantSearch_withoutLimit_returnsAtMost50Rows() {
        for (int i = 0; i < 51; i++) {
            persist(instant(String.format("00000000-0000-0000-0000-00000001%04d", i), "E2E-L" + i, "COMPLETED",
                    "DE89370400440532013000", "FR7630006000011234567890189", Timestamp.valueOf("2026-10-15 09:15:00"), "EUR"));
        }

        assertThat(instantRepository.search(new InstantPaymentSearchRequest())).hasSize(50);
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

    // ---------------- suspended ----------------

    @Test
    void suspendedSearch_appliesParameterizedFilters() {
        persist(
                suspended("00000000-0000-0000-0000-000000000611", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000612", "DE89370400440532013000", "SANCTIONS_SCREENING",
                        "PENDING", Timestamp.valueOf("2026-10-14 09:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000613", "DE89370400440532013000", "AML_THRESHOLD",
                        "RELEASED", Timestamp.valueOf("2026-10-13 08:00:00"), Timestamp.valueOf("2026-10-13 15:00:00")));

        var results = suspendedRepository.search(SuspendedPaymentSearchCriteria.from("DE89370400440532013000",
                "aml_threshold", "pending", "2026-10-01T00:00:00", "2026-10-31T00:00:00", 5));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000611"));
        assertThat(results.get(0).holdReason()).isEqualTo("AML_THRESHOLD");
        assertThat(results.get(0).heldAt()).isEqualTo(LocalDateTime.of(2026, 10, 14, 8, 0, 0));
    }

    @Test
    void suspendedSearch_debtorIbanFilter_isTrimmedExactAndCaseSensitive() {
        persist(suspended("00000000-0000-0000-0000-000000000615", "DE89370400440532013000", "AML_THRESHOLD",
                "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));

        assertThat(suspendedRepository.search(suspendedByDebtorIban("  DE89370400440532013000 "))).hasSize(1);
        assertThat(suspendedRepository.search(suspendedByDebtorIban("DE89"))).isEmpty();
        assertThat(suspendedRepository.search(suspendedByDebtorIban("de89370400440532013000"))).isEmpty();
    }

    @Test
    void suspendedSearch_ordersByHeldAtDescending() {
        persist(
                suspended("00000000-0000-0000-0000-000000000616", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-12 08:00:00"), null),
                suspended("00000000-0000-0000-0000-000000000617", "DE89370400440532013000", "AML_THRESHOLD",
                        "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));

        assertThat(suspendedRepository.search(SuspendedPaymentSearchCriteria.from(null, null, null, null, null, null)))
                .extracting(p -> p.id().value().toString())
                .containsExactly("00000000-0000-0000-0000-000000000617", "00000000-0000-0000-0000-000000000616");
    }

    @Test
    void suspendedSearch_heldBoundNotInLegacyTimestampFormat_returnsNoRows() {
        persist(suspended("00000000-0000-0000-0000-000000000618", "DE89370400440532013000", "AML_THRESHOLD",
                "PENDING", Timestamp.valueOf("2026-10-14 08:00:00"), null));

        assertThat(suspendedRepository.search(SuspendedPaymentSearchCriteria.from(null, null, null, "2026-10-01", null,
                null))).isEmpty();
    }

    @Test
    void suspendedFindById_foundAndMissing() {
        persist(suspended("00000000-0000-0000-0000-000000000614", "DE89370400440532013000", "AML_THRESHOLD",
                "RELEASED", Timestamp.valueOf("2026-10-14 08:00:00"), Timestamp.valueOf("2026-10-13 15:45:00")));

        var found = suspendedRepository.findById(PaymentId.parse("00000000-0000-0000-0000-000000000614")).orElseThrow();
        assertThat(found.reviewStatus()).isEqualTo("RELEASED");
        assertThat(found.releasedAt()).isEqualTo(LocalDateTime.of(2026, 10, 13, 15, 45, 0));
        assertThat(suspendedRepository.findById(PaymentId.parse("11111111-1111-1111-1111-111111111111"))).isEmpty();
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
    void inboundSearch_creditorIbanIsExact_originBicMatchesStoredValueIgnoringCase() {
        persist(inbound("00000000-0000-0000-0000-000000000625", "CTR-5", "DE89370400440532013000", "bnpafrpp",
                "RECEIVED", "EUR", Date.valueOf("2026-10-15"), null));

        InboundPaymentSearchRequest byBic = new InboundPaymentSearchRequest();
        byBic.originBic = "NPAF";
        InboundPaymentSearchRequest byPartialIban = new InboundPaymentSearchRequest();
        byPartialIban.creditorIban = "DE89";

        assertThat(inboundRepository.search(byBic)).extracting(r -> r.creditTransferRef).containsExactly("CTR-5");
        assertThat(inboundRepository.search(byPartialIban)).isEmpty();
    }

    @Test
    void inboundSearch_ordersByValueDateThenCreatedAtDescending() {
        InboundPayment older = inbound("00000000-0000-0000-0000-000000000626", "CTR-OLD", "DE89370400440532013000",
                "BNPAFRPP", "RECEIVED", "EUR", Date.valueOf("2026-10-14"), null);
        InboundPayment sameDayEarly = inbound("00000000-0000-0000-0000-000000000627", "CTR-EARLY",
                "DE89370400440532013000", "BNPAFRPP", "RECEIVED", "EUR", Date.valueOf("2026-10-16"), null);
        InboundPayment sameDayLate = inbound("00000000-0000-0000-0000-000000000628", "CTR-LATE",
                "DE89370400440532013000", "BNPAFRPP", "RECEIVED", "EUR", Date.valueOf("2026-10-16"), null);
        sameDayLate.createdAt = Timestamp.valueOf("2026-10-16 18:00:00");
        persist(older, sameDayEarly, sameDayLate);

        assertThat(inboundRepository.search(new InboundPaymentSearchRequest())).extracting(r -> r.creditTransferRef)
                .containsExactly("CTR-LATE", "CTR-EARLY", "CTR-OLD");
    }

    @Test
    void inboundSearch_limitAbove200_isNotCapped() {
        for (int i = 0; i < 201; i++) {
            persist(inbound(String.format("00000000-0000-0000-0000-00000002%04d", i), "CTR-L" + i,
                    "DE89370400440532013000", "BNPAFRPP", "RECEIVED", "EUR", Date.valueOf("2026-10-15"), null));
        }
        InboundPaymentSearchRequest request = new InboundPaymentSearchRequest();
        request.limit = 500;

        assertThat(inboundRepository.search(request)).hasSize(201);
        assertThat(inboundRepository.search(new InboundPaymentSearchRequest())).hasSize(50);
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
    void outboundSearch_debtorIbanIsExact_destinationBicDoesNotMatchLowercaseStoredValue() {
        persist(
                outbound("00000000-0000-0000-0000-000000000635", "OUT-UPPER", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-16")),
                outbound("00000000-0000-0000-0000-000000000636", "OUT-LOWER", "DE89370400440532013000", "abnanl2a",
                        "SHA", "SENT", Date.valueOf("2026-10-16")));

        OutboundPaymentSearchRequest byBic = new OutboundPaymentSearchRequest();
        byBic.destinationBic = "abna";
        OutboundPaymentSearchRequest byPartialIban = new OutboundPaymentSearchRequest();
        byPartialIban.debtorIban = "DE89";

        assertThat(outboundRepository.search(byBic)).extracting(p -> p.instructionId).containsExactly("OUT-UPPER");
        assertThat(outboundRepository.search(byPartialIban)).isEmpty();
    }

    @Test
    void outboundSearch_ordersByRequestedExecutionDateDescending() {
        persist(
                outbound("00000000-0000-0000-0000-000000000637", "OUT-EARLY", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-10")),
                outbound("00000000-0000-0000-0000-000000000638", "OUT-LATE", "DE89370400440532013000", "ABNANL2A",
                        "SHA", "SENT", Date.valueOf("2026-10-20")));

        assertThat(outboundRepository.search(new OutboundPaymentSearchRequest())).extracting(p -> p.instructionId)
                .containsExactly("OUT-LATE", "OUT-EARLY");
    }

    @Test
    void outboundSearch_limitAbove200_isCappedAt200() {
        for (int i = 0; i < 201; i++) {
            persist(outbound(String.format("00000000-0000-0000-0000-00000003%04d", i), "OUT-L" + i,
                    "DE89370400440532013000", "ABNANL2A", "SHA", "SENT", Date.valueOf("2026-10-16")));
        }
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.limit = 500;

        assertThat(outboundRepository.search(request)).hasSize(200);
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

        assertThat(recallRepository.search(recallCriteria(null, null, null, null, null, null, null, 5))).hasSize(50);
    }

    @Test
    void recallSearch_appliesFilters() {
        persist(
                recall("00000000-0000-0000-0000-000000000751", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")),
                recall("00000000-0000-0000-0000-000000000752", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")));

        var results = recallRepository.search(recallCriteria("r-cor", "00000000-0000-0000-0000-000000000401",
                "outbound", "ac04", "initiated", "2026-10-01T00:00:00", "2026-10-31T00:00:00", null));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).recallReference()).isEqualTo("RCL-T1");
    }

    @Test
    void recallSearch_dateOnlyInitiatedTo_excludesThatDay() {
        persist(recall("00000000-0000-0000-0000-000000000754", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18")));

        assertThat(recallRepository.search(recallInitiatedTo("2026-10-16"))).isEmpty();
        assertThat(recallRepository.search(recallInitiatedTo("2026-10-17"))).hasSize(1);
    }

    @Test
    void recallSearch_unparseableInitiatedBounds_areIgnored() {
        persist(recall("00000000-0000-0000-0000-000000000755", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18")));

        assertThat(recallRepository.search(recallCriteria(null, null, null, null, null, "last week", "16/10/2026",
                null))).hasSize(1);
    }

    @Test
    void recallSearch_ordersByInitiatedAtDescending() {
        persist(
                recall("00000000-0000-0000-0000-000000000756", "R-REJ", Timestamp.valueOf("2026-10-14 19:00:00"),
                        Date.valueOf("2026-10-16")),
                recall("00000000-0000-0000-0000-000000000757", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                        Date.valueOf("2026-10-18")));

        assertThat(recallRepository.search(recallCriteria(null, null, null, null, null, null, null, null)))
                .extracting(r -> r.recallType())
                .containsExactly("R-COR", "R-REJ");
    }

    @Test
    void recallFindById_foundAndMissing() {
        persist(recall("00000000-0000-0000-0000-000000000753", "R-COR", Timestamp.valueOf("2026-10-16 12:00:00"),
                Date.valueOf("2026-10-18")));

        var found = recallRepository.findById(PaymentId.parse("00000000-0000-0000-0000-000000000753")).orElseThrow();
        assertThat(found.recallType()).isEqualTo("R-COR");
        assertThat(found.deadline()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(recallRepository.findById(PaymentId.parse("11111111-1111-1111-1111-111111111111"))).isEmpty();
    }

    // ---------------- helpers ----------------

    private void persist(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    private SuspendedPaymentSearchCriteria suspendedByDebtorIban(String debtorIban) {
        return SuspendedPaymentSearchCriteria.from(debtorIban, null, null, null, null, null);
    }

    private RecallSearchCriteria recallInitiatedTo(String initiatedTo) {
        return recallCriteria(null, null, null, null, null, null, initiatedTo, null);
    }

    private RecallSearchCriteria recallCriteria(String recallType, String originalPaymentId, String originalPaymentType,
                                                String reasonCode, String status, String initiatedFrom,
                                                String initiatedTo, Integer limit) {
        return RecallSearchCriteria.from(recallType, originalPaymentId, originalPaymentType, reasonCode, status,
                initiatedFrom, initiatedTo, limit);
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
