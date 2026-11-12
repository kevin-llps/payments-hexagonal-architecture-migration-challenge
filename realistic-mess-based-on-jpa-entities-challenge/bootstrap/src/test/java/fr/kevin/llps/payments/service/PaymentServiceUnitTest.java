package fr.kevin.llps.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InboundPaymentSearchRequest;
import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentSearchRequest;
import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.dto.RecallResponse;
import fr.kevin.llps.payments.dto.RecallSearchRequest;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.entity.Recall;
import fr.kevin.llps.payments.repository.InboundPaymentRepository;
import fr.kevin.llps.payments.repository.InstantPaymentRepository;
import fr.kevin.llps.payments.repository.OutboundPaymentRepository;
import fr.kevin.llps.payments.repository.RecallRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private InstantPaymentRepository instantRepository;
    @Mock
    private InboundPaymentRepository inboundRepository;
    @Mock
    private OutboundPaymentRepository outboundRepository;
    @Mock
    private RecallRepository recallRepository;

    @InjectMocks
    private InstantPaymentService instantService;
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

    @Test
    void instantGetById_delegatesToRepository() {
        when(instantRepository.findById("1")).thenReturn(new InstantPaymentResponse());
        assertThat(instantService.getById("1")).isNotNull();
        assertThat(instantService.getById("2")).isNull();
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

    // ---------------- characterization: Instant IBAN filter validation (BR-INS-01, BR-INS-02, BR-SHR-04) ----------------

    @Test
    void should_ignore_blank_iban_filters_and_delegate_to_repository() {
        // GIVEN
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.debtorIban = "   ";
        request.creditorIban = "";
        when(instantRepository.search(request)).thenReturn(List.of());

        // WHEN
        List<InstantPaymentResponse> result = instantService.search(request);

        // THEN
        assertThat(result).isEmpty();
        verify(instantRepository).search(request);
    }

    @Test
    void should_report_debtor_iban_first_when_both_iban_filters_are_invalid() {
        // GIVEN
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.debtorIban = "DE1";
        request.creditorIban = "FR1";

        // WHEN / THEN
        assertThatThrownBy(() -> instantService.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid debtor IBAN");
        verifyNoInteractions(instantRepository);
    }

    @Test
    void should_not_query_repository_when_creditor_iban_is_invalid() {
        // GIVEN
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.debtorIban = "DE89370400440532013000";
        request.creditorIban = "FR1";

        // WHEN / THEN
        assertThatThrownBy(() -> instantService.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid creditor IBAN");
        verifyNoInteractions(instantRepository);
    }

    @Test
    void should_accept_iban_filter_containing_spaces_when_length_without_spaces_is_valid() {
        // GIVEN
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.debtorIban = "DE89 3704 0044 0532 0130 00";
        when(instantRepository.search(request)).thenReturn(List.of());

        // WHEN
        List<InstantPaymentResponse> result = instantService.search(request);

        // THEN
        assertThat(result).isEmpty();
        verify(instantRepository).search(request);
    }

    // ---------------- characterization: Recall type filter validation (BR-RCL-01) ----------------

    @ParameterizedTest(name = "[{index}] recallType \"{0}\" accepted")
    @ValueSource(strings = {"R-REJ", "R-COR", "R-UPD", " r-rej ", "r-cor", "R-upd"})
    void should_accept_allowed_recall_types_trimmed_and_case_insensitive(String recallType) {
        // GIVEN
        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = recallType;
        when(recallRepository.search(request)).thenReturn(List.of());

        // WHEN
        List<RecallResponse> result = recallService.search(request);

        // THEN
        assertThat(result).isEmpty();
        verify(recallRepository).search(request);
    }

    @Test
    void should_reject_unknown_recall_type_with_raw_input_in_message() {
        // GIVEN
        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = " nope ";

        // WHEN / THEN
        assertThatThrownBy(() -> recallService.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown recall type:  nope ");
        verifyNoInteractions(recallRepository);
    }

    @Test
    void should_ignore_blank_recall_type_filter() {
        // GIVEN
        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = "  ";
        when(recallRepository.search(request)).thenReturn(List.of());

        // WHEN
        List<RecallResponse> result = recallService.search(request);

        // THEN
        assertThat(result).isEmpty();
        verify(recallRepository).search(request);
    }

    // ---------------- characterization: Outbound search fee policy (BR-OUT-06, BR-OUT-08) ----------------

    @ParameterizedTest(name = "[{index}] {1} on {0} -> search fee {2}")
    @CsvSource(value = {
            "1000.00, OUR, 10.00",
            "15600.75, OUR, 156.01",
            "0.50, OUR, 0.01",
            "780.00, SHA, 0.30",
            "95.00, BEN, 0",
            "95.00, XYZ, 0.30",
            "95.00, NULL, 0.30",
            "NULL, OUR, 0",
            "NULL, NULL, 0"
    }, nullValues = "NULL")
    void should_compute_search_fee_with_service_fee_table(String amount, String chargesType, String expectedFee) {
        // GIVEN
        OutboundPayment payment = outboundPayment("00000000-0000-0000-0000-000000000415", amount, chargesType);
        when(outboundRepository.search(any())).thenReturn(List.of(payment));

        // WHEN
        List<OutboundPaymentResponse> result = outboundService.search(new OutboundPaymentSearchRequest());

        // THEN
        assertThat(result).singleElement()
                .satisfies(response -> assertThat(response.estimatedFee).isEqualByComparingTo(expectedFee));
    }

    @Test
    void should_keep_search_fee_scale_of_two_for_our_payments() {
        // GIVEN
        OutboundPayment payment = outboundPayment("00000000-0000-0000-0000-000000000416", "15600.75", "OUR");
        when(outboundRepository.search(any())).thenReturn(List.of(payment));

        // WHEN
        List<OutboundPaymentResponse> result = outboundService.search(new OutboundPaymentSearchRequest());

        // THEN
        assertThat(result.get(0).estimatedFee).isEqualTo(new BigDecimal("156.01"));
    }

    // ---------------- characterization: Inbound age in days in details (BR-INB-07, BR-INB-08) ----------------

    @Test
    void should_return_negative_age_in_days_when_value_date_is_in_the_future() {
        // GIVEN
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        response.valueDate = LocalDate.now().plusDays(5).toString();
        when(inboundRepository.findById("i1")).thenReturn(response);

        // WHEN
        InboundPaymentResponse result = inboundService.getById("i1");

        // THEN
        assertThat(result.ageInDays).isEqualTo(-5L);
    }

    @Test
    void should_leave_age_in_days_null_when_value_date_is_null() {
        // GIVEN
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        response.valueDate = null;
        when(inboundRepository.findById("i1")).thenReturn(response);

        // WHEN
        InboundPaymentResponse result = inboundService.getById("i1");

        // THEN
        assertThat(result.ageInDays).isNull();
    }

    @Test
    void should_not_compute_age_in_days_in_service_search() {
        // GIVEN
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        response.valueDate = LocalDate.now().minusDays(3).toString();
        when(inboundRepository.search(any())).thenReturn(List.of(response));

        // WHEN
        List<InboundPaymentResponse> result = inboundService.search(new InboundPaymentSearchRequest());

        // THEN
        assertThat(result.get(0).ageInDays).isNull();
    }
}
