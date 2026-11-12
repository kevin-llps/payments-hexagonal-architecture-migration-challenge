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

import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.repository.OutboundPaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private OutboundPaymentRepository outboundRepository;

    @InjectMocks
    private OutboundPaymentService outboundService;

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
}
