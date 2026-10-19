package fr.kevin.llps.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import fr.kevin.llps.payments.model.OutboundPaymentRow;
import fr.kevin.llps.payments.model.RecallRow;
import fr.kevin.llps.payments.model.SuspendedPaymentRow;
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

    @Test
    void instantGetById_delegatesToRepository() {
        when(instantRepository.findById("1")).thenReturn(new InstantPaymentResponse());
        assertThat(instantService.getById("1")).isNotNull();
        assertThat(instantService.getById("2")).isNull();
    }

    // ---------------- suspended ----------------

    @Test
    void suspendedSearch_mapsAllFields() {
        SuspendedPaymentRow row = new SuspendedPaymentRow();
        row.id = "s1";
        row.amount = new BigDecimal("100.00");
        row.currency = "EUR";
        row.debtorIban = "DE89";
        row.debtorName = "ACME";
        row.creditorIban = "FR76";
        row.creditorName = "Dupont";
        row.holdReason = "AML_THRESHOLD";
        row.complianceCaseRef = "CASE-1";
        row.reviewStatus = "PENDING";
        row.heldAt = Timestamp.valueOf("2026-10-14 08:00:00");
        row.releasedAt = null;
        when(suspendedRepository.search(any())).thenReturn(List.of(row));

        SuspendedPaymentResponse response = suspendedService.search(new SuspendedPaymentSearchRequest()).get(0);

        assertThat(response.id).isEqualTo("s1");
        assertThat(response.holdReason).isEqualTo("AML_THRESHOLD");
        assertThat(response.heldAt).isEqualTo("2026-10-14T08:00:00");
        assertThat(response.releasedAt).isNull();
    }

    @Test
    void suspendedGetById_foundAndMissing() {
        SuspendedPaymentRow row = new SuspendedPaymentRow();
        row.id = "s1";
        row.reviewStatus = "RELEASED";
        row.releasedAt = Timestamp.valueOf("2026-10-13 15:45:00");
        when(suspendedRepository.findById("s1")).thenReturn(row);
        when(suspendedRepository.findById("nope")).thenReturn(null);

        assertThat(suspendedService.getById("s1").releasedAt).isEqualTo("2026-10-13T15:45:00");
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
    void inboundGetById_missing_returnsNull() {
        when(inboundRepository.findById("nope")).thenReturn(null);
        assertThat(inboundService.getById("nope")).isNull();
    }

    // ---------------- outbound ----------------

    @Test
    void outboundSearch_computesFeePerChargesType() {
        OutboundPaymentRow our = outboundRow("o1", "1000.00", "OUR");
        OutboundPaymentRow sha = outboundRow("o2", "1000.00", "SHA");
        OutboundPaymentRow ben = outboundRow("o3", "1000.00", "BEN");
        OutboundPaymentRow nullAmount = outboundRow("o4", null, "SHA");
        when(outboundRepository.search(any())).thenReturn(List.of(our, sha, ben, nullAmount));

        List<OutboundPaymentResponse> result = outboundService.search(new OutboundPaymentSearchRequest());

        assertThat(result.get(0).estimatedFee).isEqualByComparingTo("10.00");
        assertThat(result.get(1).estimatedFee).isEqualByComparingTo("0.30");
        assertThat(result.get(2).estimatedFee).isEqualByComparingTo("0.00");
        assertThat(result.get(3).estimatedFee).isEqualByComparingTo("0.00");
    }

    @Test
    void outboundGetById_foundAndMissing() {
        when(outboundRepository.findById("o1")).thenReturn(outboundRow("o1", "10.00", "SHA"));
        when(outboundRepository.findById("nope")).thenReturn(null);

        assertThat(outboundService.getById("o1")).isNotNull();
        assertThat(outboundService.getById("nope")).isNull();
    }

    private OutboundPaymentRow outboundRow(String id, String amount, String chargesType) {
        OutboundPaymentRow row = new OutboundPaymentRow();
        row.id = id;
        row.amount = amount == null ? null : new BigDecimal(amount);
        row.currency = "EUR";
        row.chargesType = chargesType;
        row.status = "SENT";
        return row;
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
        RecallRow row = new RecallRow();
        row.id = "r1";
        row.recallReference = "RCL-1";
        row.recallType = "R-COR";
        row.originalPaymentId = "o1";
        row.originalPaymentType = "OUTBOUND";
        row.reasonCode = "AC04";
        row.amount = new BigDecimal("300.00");
        row.currency = "EUR";
        row.initiatedAt = Timestamp.valueOf("2026-10-16 12:00:00");
        row.deadline = java.sql.Date.valueOf("2026-10-18");
        row.status = "INITIATED";
        when(recallRepository.search(any())).thenReturn(List.of(row));

        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = "r-cor";
        RecallResponse response = recallService.search(request).get(0);

        assertThat(response.initiatedAt).isEqualTo("2026-10-16T12:00:00");
        assertThat(response.deadline).isEqualTo("2026-10-18");
        assertThat(response.reasonCode).isEqualTo("AC04");
    }

    @Test
    void recallGetById_foundAndMissing() {
        when(recallRepository.findById("r1")).thenReturn(new RecallRow());
        when(recallRepository.findById("nope")).thenReturn(null);

        assertThat(recallService.getById("r1")).isNotNull();
        assertThat(recallService.getById("nope")).isNull();
    }
}
