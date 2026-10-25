package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.recall.port.in.RecallSearchQuery;
import fr.kevin.llps.payments.recall.port.out.RecallSearchRepository;
import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Characterization tests of the legacy service behavior (Phase 2), carried over from the legacy
 * PaymentServiceUnitTest and re-targeted to {@link RecallQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class RecallQueryServiceCharacterizationTest {

    @Mock
    private RecallSearchRepository recallRepository;
    @InjectMocks
    private RecallQueryService recallQueries;

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
