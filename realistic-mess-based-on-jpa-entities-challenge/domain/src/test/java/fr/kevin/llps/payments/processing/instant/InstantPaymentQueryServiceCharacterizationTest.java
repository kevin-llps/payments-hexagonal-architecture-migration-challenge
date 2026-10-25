package fr.kevin.llps.payments.processing.instant;

import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentSearchQuery;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Characterization tests of the legacy service behavior (Phase 2), carried over from the legacy
 * PaymentServiceUnitTest and re-targeted to {@link InstantPaymentQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class InstantPaymentQueryServiceCharacterizationTest {

    @Mock
    private InstantPaymentSearchRepository instantRepository;
    @InjectMocks
    private InstantPaymentQueryService instantQueries;

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
}
