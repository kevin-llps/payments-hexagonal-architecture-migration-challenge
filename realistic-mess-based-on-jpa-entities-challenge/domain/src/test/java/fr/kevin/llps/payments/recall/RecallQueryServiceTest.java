package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.recall.port.in.RecallSearchQuery;
import fr.kevin.llps.payments.recall.port.out.RecallSearchRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import fr.kevin.llps.payments.shared.domain.SearchLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecallQueryServiceTest {

    private static final PaymentId ID = PaymentId.parse("00000000-0000-0000-0000-000000000501");

    @Mock
    private RecallSearchRepository repository;

    @InjectMocks
    private RecallQueryService service;

    @Test
    void search_should_return_the_recalls_matching_the_criteria_built_from_the_query() {
        // GIVEN
        RecallSearchQuery query = new RecallSearchQuery("r-cor", null, "outbound", null, null,
                "2026-10-01", null, 5);
        RecallSearchCriteria expectedCriteria = new RecallSearchCriteria("R-COR", null, "OUTBOUND", null, null,
                LocalDateTime.of(2026, 10, 1, 0, 0), null, new SearchLimit(50));
        List<Recall> recalls = List.of(recall());
        when(repository.search(expectedCriteria)).thenReturn(recalls);

        // WHEN
        List<Recall> result = service.search(query);

        // THEN
        assertThat(result).isEqualTo(recalls);
    }

    @Test
    void get_should_return_the_recall_found_by_id() {
        // GIVEN
        Recall recall = recall();
        when(repository.findById(ID)).thenReturn(Optional.of(recall));

        // WHEN
        Optional<Recall> result = service.get(ID);

        // THEN
        assertThat(result).contains(recall);
    }

    private static Recall recall() {
        return new Recall(ID, "RCL-1", "R-COR", new OriginalPaymentRef(null, "OUTBOUND"), "AC04", null, "EUR",
                null, null, "INITIATED", null);
    }
}
