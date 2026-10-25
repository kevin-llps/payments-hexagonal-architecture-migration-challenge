package fr.kevin.llps.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.compliancehold.SuspendedPayment;
import fr.kevin.llps.payments.compliancehold.SuspendedPaymentQueryService;
import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentSearchQuery;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentSearchRepository;
import fr.kevin.llps.payments.processing.inbound.InboundPayment;
import fr.kevin.llps.payments.processing.inbound.InboundPaymentQueryService;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentSearchQuery;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentSearchRepository;
import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.processing.outbound.ChargesType;
import fr.kevin.llps.payments.processing.outbound.DetailFeeEstimate;
import fr.kevin.llps.payments.processing.outbound.OutboundPayment;
import fr.kevin.llps.payments.processing.outbound.OutboundPaymentQueryService;
import fr.kevin.llps.payments.processing.outbound.SearchFeeEstimate;
import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentSearchQuery;
import fr.kevin.llps.payments.processing.outbound.port.in.PricedOutboundPayment;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentSearchRepository;
import fr.kevin.llps.payments.processing.instant.InstantPaymentQueryService;
import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentSearchQuery;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentSearchRepository;
import fr.kevin.llps.payments.recall.RecallQueryService;
import fr.kevin.llps.payments.recall.port.in.RecallSearchQuery;
import fr.kevin.llps.payments.recall.Recall;
import fr.kevin.llps.payments.recall.port.out.RecallSearchRepository;
import fr.kevin.llps.payments.shared.domain.DomainInvariantViolationException;
import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private InstantPaymentSearchRepository instantRepository;
    @Mock
    private SuspendedPaymentSearchRepository suspendedRepository;
    @Mock
    private InboundPaymentSearchRepository inboundRepository;
    @Mock
    private OutboundPaymentSearchRepository outboundRepository;
    @Mock
    private RecallSearchRepository recallRepository;

    @InjectMocks
    private InstantPaymentQueryService instantQueries;
    @InjectMocks
    private SuspendedPaymentQueryService suspendedQueries;
    @InjectMocks
    private RecallQueryService recallQueries;
    // the real system clock, as the legacy service used LocalDate.now()
    private InboundPaymentQueryService inboundQueries;
    private OutboundPaymentQueryService outboundQueries;

    @BeforeEach
    void wireInboundQueries() {
        inboundQueries = new InboundPaymentQueryService(inboundRepository, Clock.systemDefaultZone());
    }

    @BeforeEach
    void wireOutboundQueries() {
        outboundQueries = new OutboundPaymentQueryService(outboundRepository, new SearchFeeEstimate(),
                new DetailFeeEstimate());
    }

    // ---------------- instant ----------------

    @Test
    void instantSearch_validIbans_delegatesToRepository() {
        when(instantRepository.search(any())).thenReturn(List.of(instant("00000000-0000-0000-0000-000000000101")));

        List<InstantPayment> result = instantQueries.search(instantQuery(null, null));

        assertThat(result).hasSize(1);
    }

    @Test
    void instantSearch_invalidDebtorIban_throws() {
        assertThatThrownBy(() -> instantQueries.search(instantQuery("DE1", null)))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessage("Invalid debtor IBAN");
    }

    @Test
    void instantSearch_invalidCreditorIban_throws() {
        assertThatThrownBy(() -> instantQueries.search(instantQuery(null, "X")))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessage("Invalid creditor IBAN");
    }

    @ParameterizedTest(name = "[{index}] IBAN filter \"{0}\" is accepted")
    @ValueSource(strings = {"AAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "DE89 3704 0044 0532 0130 00", "   "})
    void instantSearch_ibanFilterWithin15To34CharsIgnoringSpacesOrBlank_isAccepted(String iban) {
        when(instantRepository.search(any())).thenReturn(List.of());

        assertThat(instantQueries.search(instantQuery(iban, iban))).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] IBAN filter \"{0}\" is rejected")
    @ValueSource(strings = {"AAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "DE89 3704 0044"})
    void instantSearch_ibanFilterOutside15To34Chars_isRejected(String iban) {
        assertThatThrownBy(() -> instantQueries.search(instantQuery(null, iban)))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessage("Invalid creditor IBAN");
    }

    @Test
    void instantGetById_delegatesToRepository() {
        PaymentId found = PaymentId.parse("00000000-0000-0000-0000-000000000101");
        PaymentId missing = PaymentId.parse("00000000-0000-0000-0000-000000000102");
        when(instantRepository.findById(found)).thenReturn(Optional.of(instant(found.value().toString())));
        when(instantRepository.findById(missing)).thenReturn(Optional.empty());

        assertThat(instantQueries.get(found)).isPresent();
        assertThat(instantQueries.get(missing)).isEmpty();
    }

    private static InstantPaymentSearchQuery instantQuery(String debtorIban, String creditorIban) {
        return new InstantPaymentSearchQuery(null, debtorIban, creditorIban, null, null, null, null, null);
    }

    private static InstantPayment instant(String id) {
        return new InstantPayment(PaymentId.parse(id), null, null, Money.of(new BigDecimal("1.00"), "EUR"), null, null,
                null, null, null, null, "SCT INST", null);
    }

    // ---------------- suspended ----------------

    // suspendedSearch_mapsAllFields moved to PaymentControllerWebTest (rendering is a REST adapter concern)
    @Test
    void suspendedSearch_returnsRepositoryRows() {
        SuspendedPayment payment = suspended("00000000-0000-0000-0000-000000000201");
        when(suspendedRepository.search(any())).thenReturn(List.of(payment));

        assertThat(suspendedQueries.search(new SuspendedPaymentSearchQuery(null, null, null, null, null, null)))
                .containsExactly(payment);
    }

    // releasedAt rendering moved to PaymentControllerWebTest.suspendedDetails_formatsReleasedAt
    @Test
    void suspendedGetById_foundAndMissing() {
        PaymentId found = PaymentId.parse("00000000-0000-0000-0000-000000000202");
        PaymentId missing = PaymentId.parse("00000000-0000-0000-0000-000000000299");
        when(suspendedRepository.findById(found)).thenReturn(Optional.of(suspended(found.value().toString())));
        when(suspendedRepository.findById(missing)).thenReturn(Optional.empty());

        assertThat(suspendedQueries.get(found)).isPresent();
        assertThat(suspendedQueries.get(missing)).isEmpty();
    }

    private static SuspendedPayment suspended(String id) {
        return new SuspendedPayment(PaymentId.parse(id), Money.of(new BigDecimal("100.00"), "EUR"), null, null, null,
                null, "AML_THRESHOLD", null, "RELEASED", null, null, null);
    }

    // ---------------- inbound ----------------

    @Test
    void inboundSearch_delegatesToRepository() {
        when(inboundRepository.search(any())).thenReturn(List.of(inbound("00000000-0000-0000-0000-000000000301", null)));
        assertThat(inboundQueries.search(new InboundPaymentSearchQuery(null, null, null, null, null, null, null)))
                .hasSize(1);
    }

    // moved from PaymentControllerWebTest: the age shown in search results is computed by the query service
    @Test
    void inboundSearch_computesAgeInDays() {
        when(inboundRepository.search(any())).thenReturn(List.of(
                inbound("00000000-0000-0000-0000-000000000301", LocalDate.now().minusDays(2)),
                inbound("00000000-0000-0000-0000-000000000302", LocalDate.now().plusDays(5)),
                inbound("00000000-0000-0000-0000-000000000303", null)));

        assertThat(inboundQueries.search(new InboundPaymentSearchQuery(null, null, null, null, null, null, null)))
                .extracting(view -> view.ageInDays())
                .containsExactly(2L, -5L, null);
    }

    @Test
    void inboundGetById_computesAgeInDays() {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000301");
        when(inboundRepository.findById(id)).thenReturn(Optional.of(inbound(id.value().toString(),
                LocalDate.now().minusDays(3))));

        assertThat(inboundQueries.get(id).orElseThrow().ageInDays()).isEqualTo(3L);
    }

    @Test
    void inboundGetById_futureValueDate_hasNegativeAgeInDays() {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000302");
        when(inboundRepository.findById(id)).thenReturn(Optional.of(inbound(id.value().toString(),
                LocalDate.now().plusDays(4))));

        assertThat(inboundQueries.get(id).orElseThrow().ageInDays()).isEqualTo(-4L);
    }

    @Test
    void inboundGetById_withoutValueDate_leavesAgeInDaysNull() {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000303");
        when(inboundRepository.findById(id)).thenReturn(Optional.of(inbound(id.value().toString(), null)));

        assertThat(inboundQueries.get(id).orElseThrow().ageInDays()).isNull();
    }

    @Test
    void inboundGetById_missing_returnsNull() {
        PaymentId missing = PaymentId.parse("00000000-0000-0000-0000-000000000399");
        when(inboundRepository.findById(missing)).thenReturn(Optional.empty());
        assertThat(inboundQueries.get(missing)).isEmpty();
    }

    private static InboundPayment inbound(String id, LocalDate valueDate) {
        return new InboundPayment(PaymentId.parse(id), null, Money.of(new BigDecimal("1.00"), "EUR"), null, null, null,
                valueDate, null, null, null);
    }

    // ---------------- outbound ----------------

    @Test
    void outboundSearch_computesFeePerChargesType() {
        OutboundPayment our = outboundPayment("00000000-0000-0000-0000-000000000411", "1000.00", "OUR");
        OutboundPayment sha = outboundPayment("00000000-0000-0000-0000-000000000412", "1000.00", "SHA");
        OutboundPayment ben = outboundPayment("00000000-0000-0000-0000-000000000413", "1000.00", "BEN");
        when(outboundRepository.search(any())).thenReturn(List.of(our, sha, ben));

        List<PricedOutboundPayment> result = outboundQueries.search(outboundQuery());

        assertThat(result.get(0).estimatedFee()).isEqualByComparingTo("10.00");
        assertThat(result.get(1).estimatedFee()).isEqualByComparingTo("0.30");
        assertThat(result.get(2).estimatedFee()).isEqualByComparingTo("0.00");
    }

    // D-01 (approved): replaces the null-amount row of outboundSearch_computesFeePerChargesType and the
    // "OUR, , 0.00" row of outboundSearch_feeEstimate_followsSearchFeeTable — a payment without amount is rejected
    @Test
    void outboundSearch_paymentWithoutAmount_isRejected() {
        assertThatThrownBy(() -> outboundPayment("00000000-0000-0000-0000-000000000414", null, "SHA"))
                .isInstanceOf(DomainInvariantViolationException.class);
    }

    @ParameterizedTest(name = "[{index}] chargesType={0}, amount={1} -> search fee {2}")
    @CsvSource({
            "OUR, 1000.00, 10.00",
            "OUR, 1234.56, 12.35",
            "OUR, 0.50, 0.01",
            "OUR, 15600.75, 156.01",
            "BEN, 1000.00, 0.00",
            "SHA, 1000.00, 0.30",
            "XYZ, 1000.00, 0.30",
            "'', 1000.00, 0.30",
            ", 1000.00, 0.30"
    })
    void outboundSearch_feeEstimate_followsSearchFeeTable(String chargesType, String amount, String expectedFee) {
        when(outboundRepository.search(any())).thenReturn(List.of(
                outboundPayment("00000000-0000-0000-0000-000000000415", amount, chargesType)));

        PricedOutboundPayment response = outboundQueries.search(outboundQuery()).get(0);

        assertThat(response.estimatedFee()).isEqualByComparingTo(expectedFee);
    }

    // outboundGetById_mapsRequestedExecutionDateAsIsoDate: OutboundPaymentService.getById was reachable from no
    // endpoint and was not migrated (approved); its date rendering and 156.01 search fee are asserted by
    // PaymentControllerWebTest.outboundSearch_mapsRequestedExecutionDateAsIsoDate and the "OUR, 15600.75" row above.

    // details are priced with the detail tariff, as the details endpoint always was
    @Test
    void outboundGetById_foundAndMissing() {
        PaymentId found = PaymentId.parse("00000000-0000-0000-0000-000000000401");
        PaymentId missing = PaymentId.parse("00000000-0000-0000-0000-000000000499");
        when(outboundRepository.findById(found))
                .thenReturn(Optional.of(outboundPayment(found.value().toString(), "10.00", "SHA")));
        when(outboundRepository.findById(missing)).thenReturn(Optional.empty());

        assertThat(outboundQueries.get(found)).isPresent();
        assertThat(outboundQueries.get(missing)).isEmpty();
    }

    private static OutboundPaymentSearchQuery outboundQuery() {
        return new OutboundPaymentSearchQuery(null, null, null, null, null, null, null);
    }

    private static OutboundPayment outboundPayment(String id, String amount, String chargesType) {
        return new OutboundPayment(PaymentId.parse(id), null,
                Money.of(amount == null ? null : new BigDecimal(amount), "EUR"), null, null, null, null,
                new ChargesType(chargesType), "SENT", null);
    }

    // ---------------- recalls ----------------

    @Test
    void recallSearch_invalidType_throws() {
        RecallSearchQuery query = recallQuery("NOPE");

        assertThatThrownBy(() -> recallQueries.search(query))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("Unknown recall type");
    }

    @ParameterizedTest(name = "[{index}] recallType \"{0}\" is accepted")
    @ValueSource(strings = {"R-REJ", "r-cor", " R-UPD ", "   "})
    void recallSearch_knownTypeIgnoringCaseAndSpacesOrBlank_isAccepted(String recallType) {
        when(recallRepository.search(any())).thenReturn(List.of());

        assertThat(recallQueries.search(recallQuery(recallType))).isEmpty();
    }

    @Test
    void recallSearch_invalidType_messageEchoesRequestedValue() {
        assertThatThrownBy(() -> recallQueries.search(recallQuery("R-XXX")))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessage("Unknown recall type: R-XXX");
    }

    // recallSearch_mapsFieldsAndFormatsDates moved to PaymentControllerWebTest (rendering is a REST adapter concern)

    @Test
    void recallGetById_foundAndMissing() {
        PaymentId found = PaymentId.parse("00000000-0000-0000-0000-000000000501");
        PaymentId missing = PaymentId.parse("00000000-0000-0000-0000-000000000599");
        when(recallRepository.findById(found)).thenReturn(Optional.of(
                new Recall(found, null, null, null, null, null, null, null, null, null, null)));
        when(recallRepository.findById(missing)).thenReturn(Optional.empty());

        assertThat(recallQueries.get(found)).isPresent();
        assertThat(recallQueries.get(missing)).isEmpty();
    }

    private static RecallSearchQuery recallQuery(String recallType) {
        return new RecallSearchQuery(recallType, null, null, null, null, null, null, null);
    }
}
