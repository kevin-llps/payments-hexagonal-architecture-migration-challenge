package fr.kevin.llps.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentSearchRequest;
import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.dto.RecallResponse;
import fr.kevin.llps.payments.dto.RecallSearchRequest;
import fr.kevin.llps.payments.dto.SuspendedPaymentResponse;
import fr.kevin.llps.payments.dto.SuspendedPaymentSearchRequest;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.entity.Recall;
import fr.kevin.llps.payments.entity.SuspendedPayment;
import fr.kevin.llps.payments.repository.InboundPaymentRepository;
import fr.kevin.llps.payments.repository.InstantPaymentRepository;
import fr.kevin.llps.payments.repository.OutboundPaymentRepository;
import fr.kevin.llps.payments.repository.RecallRepository;
import fr.kevin.llps.payments.repository.SuspendedPaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private InstantPaymentRepository instantRepository;
    @Mock
    private SuspendedPaymentRepository suspendedRepository;
    @Mock
    private InboundPaymentRepository inboundRepository;
    @Mock
    private OutboundPaymentRepository outboundRepository;
    @Mock
    private RecallRepository recallRepository;

    @InjectMocks
    private InstantPaymentService instantService;
    @InjectMocks
    private SuspendedPaymentService suspendedService;
    @InjectMocks
    private InboundPaymentService inboundService;
    @InjectMocks
    private OutboundPaymentService outboundService;
    @InjectMocks
    private RecallService recallService;

    // ---------------- instant ----------------

    @Test
    void instantSearch_validIbans_delegatesToRepository() {
        InstantPaymentResponse response = new InstantPaymentResponse();
        response.id = "1";
        when(instantRepository.search(any())).thenReturn(List.of(response));

        List<InstantPaymentResponse> result = instantService.search(new InstantPaymentSearchRequest());

        assertThat(result).hasSize(1);
    }

    @Test
    void instantSearch_invalidDebtorIban_throws() {
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.debtorIban = "DE1";

        assertThatThrownBy(() -> instantService.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid debtor IBAN");
    }

    @Test
    void instantSearch_invalidCreditorIban_throws() {
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.creditorIban = "X";

        assertThatThrownBy(() -> instantService.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid creditor IBAN");
    }

    @ParameterizedTest(name = "[{index}] IBAN filter \"{0}\" is accepted")
    @ValueSource(strings = {"AAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "DE89 3704 0044 0532 0130 00", "   "})
    void instantSearch_ibanFilterWithin15To34CharsIgnoringSpacesOrBlank_isAccepted(String iban) {
        when(instantRepository.search(any())).thenReturn(List.of());
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.debtorIban = iban;
        request.creditorIban = iban;

        assertThat(instantService.search(request)).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] IBAN filter \"{0}\" is rejected")
    @ValueSource(strings = {"AAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "DE89 3704 0044"})
    void instantSearch_ibanFilterOutside15To34Chars_isRejected(String iban) {
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.creditorIban = iban;

        assertThatThrownBy(() -> instantService.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid creditor IBAN");
    }

    @Test
    void instantGetById_delegatesToRepository() {
        when(instantRepository.findById("1")).thenReturn(new InstantPaymentResponse());
        assertThat(instantService.getById("1")).isNotNull();
        assertThat(instantService.getById("2")).isNull();
    }

    // ---------------- suspended ----------------

    @Test
    void suspendedSearch_mapsAllFields() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000201");
        SuspendedPayment payment = new SuspendedPayment();
        payment.id = id;
        payment.amount = new BigDecimal("100.00");
        payment.currency = "EUR";
        payment.debtorIban = "DE89";
        payment.debtorName = "ACME";
        payment.creditorIban = "FR76";
        payment.creditorName = "Dupont";
        payment.holdReason = "AML_THRESHOLD";
        payment.complianceCaseRef = "CASE-1";
        payment.reviewStatus = "PENDING";
        payment.heldAt = Timestamp.valueOf("2026-10-14 08:00:00");
        payment.releasedAt = null;
        when(suspendedRepository.search(any())).thenReturn(List.of(payment));

        SuspendedPaymentResponse response = suspendedService.search(new SuspendedPaymentSearchRequest()).get(0);

        assertThat(response.id).isEqualTo(id.toString());
        assertThat(response.holdReason).isEqualTo("AML_THRESHOLD");
        assertThat(response.heldAt).isEqualTo("2026-10-14T08:00:00");
        assertThat(response.releasedAt).isNull();
    }

    @Test
    void suspendedGetById_foundAndMissing() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000202");
        SuspendedPayment payment = new SuspendedPayment();
        payment.id = id;
        payment.reviewStatus = "RELEASED";
        payment.releasedAt = Timestamp.valueOf("2026-10-13 15:45:00");
        when(suspendedRepository.findById(id.toString())).thenReturn(payment);
        when(suspendedRepository.findById("nope")).thenReturn(null);

        assertThat(suspendedService.getById(id.toString()).releasedAt).isEqualTo("2026-10-13T15:45:00");
        assertThat(suspendedService.getById("nope")).isNull();
    }

    // ---------------- inbound ----------------

    @Test
    void inboundSearch_delegatesToRepository() {
        when(inboundRepository.search(any())).thenReturn(List.of(new InboundPaymentResponse()));
        assertThat(inboundService.search(null)).hasSize(1);
    }

    @Test
    void inboundGetById_computesAgeInDays() {
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        response.valueDate = LocalDate.now().minusDays(3).toString();
        when(inboundRepository.findById("i1")).thenReturn(response);

        InboundPaymentResponse result = inboundService.getById("i1");

        assertThat(result.ageInDays).isEqualTo(3L);
    }

    @Test
    void inboundGetById_futureValueDate_hasNegativeAgeInDays() {
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i2";
        response.valueDate = LocalDate.now().plusDays(4).toString();
        when(inboundRepository.findById("i2")).thenReturn(response);

        assertThat(inboundService.getById("i2").ageInDays).isEqualTo(-4L);
    }

    @Test
    void inboundGetById_withoutValueDate_leavesAgeInDaysNull() {
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i3";
        when(inboundRepository.findById("i3")).thenReturn(response);

        assertThat(inboundService.getById("i3").ageInDays).isNull();
    }

    @Test
    void inboundGetById_missing_returnsNull() {
        when(inboundRepository.findById("nope")).thenReturn(null);
        assertThat(inboundService.getById("nope")).isNull();
    }

    // ---------------- outbound ----------------

    @Test
    void outboundSearch_computesFeePerChargesType() {
        OutboundPayment our = outboundPayment("00000000-0000-0000-0000-000000000411", "1000.00", "OUR");
        OutboundPayment sha = outboundPayment("00000000-0000-0000-0000-000000000412", "1000.00", "SHA");
        OutboundPayment ben = outboundPayment("00000000-0000-0000-0000-000000000413", "1000.00", "BEN");
        OutboundPayment nullAmount = outboundPayment("00000000-0000-0000-0000-000000000414", null, "SHA");
        when(outboundRepository.search(any())).thenReturn(List.of(our, sha, ben, nullAmount));

        List<OutboundPaymentResponse> result = outboundService.search(new OutboundPaymentSearchRequest());

        assertThat(result.get(0).estimatedFee).isEqualByComparingTo("10.00");
        assertThat(result.get(1).estimatedFee).isEqualByComparingTo("0.30");
        assertThat(result.get(2).estimatedFee).isEqualByComparingTo("0.00");
        assertThat(result.get(3).estimatedFee).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest(name = "[{index}] chargesType={0}, amount={1} -> search fee {2}")
    @CsvSource({
            "OUR, 1000.00, 10.00",
            "OUR, 1234.56, 12.35",
            "OUR, 0.50, 0.01",
            "BEN, 1000.00, 0.00",
            "SHA, 1000.00, 0.30",
            "XYZ, 1000.00, 0.30",
            "'', 1000.00, 0.30",
            ", 1000.00, 0.30",
            "OUR, , 0.00"
    })
    void outboundSearch_feeEstimate_followsSearchFeeTable(String chargesType, String amount, String expectedFee) {
        when(outboundRepository.search(any())).thenReturn(List.of(
                outboundPayment("00000000-0000-0000-0000-000000000415", amount, chargesType)));

        OutboundPaymentResponse response = outboundService.search(new OutboundPaymentSearchRequest()).get(0);

        assertThat(response.estimatedFee).isEqualByComparingTo(expectedFee);
    }

    @Test
    void outboundGetById_mapsRequestedExecutionDateAsIsoDate() {
        OutboundPayment payment = outboundPayment("00000000-0000-0000-0000-000000000402", "15600.75", "OUR");
        payment.requestedExecutionDate = Date.valueOf("2026-10-17");
        when(outboundRepository.findById("00000000-0000-0000-0000-000000000402")).thenReturn(payment);

        OutboundPaymentResponse response = outboundService.getById("00000000-0000-0000-0000-000000000402");

        assertThat(response.requestedExecutionDate).isEqualTo("2026-10-17");
        assertThat(response.estimatedFee).isEqualByComparingTo("156.01");
    }

    @Test
    void outboundGetById_foundAndMissing() {
        when(outboundRepository.findById("00000000-0000-0000-0000-000000000401"))
                .thenReturn(outboundPayment("00000000-0000-0000-0000-000000000401", "10.00", "SHA"));
        when(outboundRepository.findById("nope")).thenReturn(null);

        assertThat(outboundService.getById("00000000-0000-0000-0000-000000000401")).isNotNull();
        assertThat(outboundService.getById("nope")).isNull();
    }

    private OutboundPayment outboundPayment(String id, String amount, String chargesType) {
        OutboundPayment payment = new OutboundPayment();
        payment.id = UUID.fromString(id);
        payment.amount = amount == null ? null : new BigDecimal(amount);
        payment.currency = "EUR";
        payment.chargesType = chargesType;
        payment.status = "SENT";
        return payment;
    }

    // ---------------- recalls ----------------

    @Test
    void recallSearch_invalidType_throws() {
        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = "NOPE";

        assertThatThrownBy(() -> recallService.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown recall type");
    }

    @ParameterizedTest(name = "[{index}] recallType \"{0}\" is accepted")
    @ValueSource(strings = {"R-REJ", "r-cor", " R-UPD ", "   "})
    void recallSearch_knownTypeIgnoringCaseAndSpacesOrBlank_isAccepted(String recallType) {
        when(recallRepository.search(any())).thenReturn(List.of());
        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = recallType;

        assertThat(recallService.search(request)).isEmpty();
    }

    @Test
    void recallSearch_invalidType_messageEchoesRequestedValue() {
        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = "R-XXX";

        assertThatThrownBy(() -> recallService.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown recall type: R-XXX");
    }

    @Test
    void recallSearch_mapsFieldsAndFormatsDates() {
        Recall recall = new Recall();
        recall.id = UUID.fromString("00000000-0000-0000-0000-000000000501");
        recall.recallReference = "RCL-1";
        recall.recallType = "R-COR";
        recall.originalPaymentId = UUID.fromString("00000000-0000-0000-0000-000000000401");
        recall.originalPaymentType = "OUTBOUND";
        recall.reasonCode = "AC04";
        recall.amount = new BigDecimal("300.00");
        recall.currency = "EUR";
        recall.initiatedAt = Timestamp.valueOf("2026-10-16 12:00:00");
        recall.deadline = Date.valueOf("2026-10-18");
        recall.status = "INITIATED";
        when(recallRepository.search(any())).thenReturn(List.of(recall));

        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = "r-cor";
        RecallResponse response = recallService.search(request).get(0);

        assertThat(response.initiatedAt).isEqualTo("2026-10-16T12:00:00");
        assertThat(response.deadline).isEqualTo("2026-10-18");
        assertThat(response.reasonCode).isEqualTo("AC04");
        assertThat(response.originalPaymentId).isEqualTo("00000000-0000-0000-0000-000000000401");
    }

    @Test
    void recallGetById_foundAndMissing() {
        Recall recall = new Recall();
        recall.id = UUID.fromString("00000000-0000-0000-0000-000000000501");
        when(recallRepository.findById("00000000-0000-0000-0000-000000000501")).thenReturn(recall);
        when(recallRepository.findById("nope")).thenReturn(null);

        assertThat(recallService.getById("00000000-0000-0000-0000-000000000501")).isNotNull();
        assertThat(recallService.getById("nope")).isNull();
    }
}
