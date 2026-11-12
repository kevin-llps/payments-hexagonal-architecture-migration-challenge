package fr.kevin.llps.payments.domain.recall;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.recall.port.out.LoadRecalls;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchCriteria;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchRecallsServiceTest {

    @Mock
    private LoadRecalls loadRecalls;

    @InjectMocks
    private SearchRecallsService searchRecallsService;

    @Test
    void should_return_the_loaded_recalls_unchanged() {
        // GIVEN (remapped from PaymentServiceUnitTest.recallSearch_mapsFieldsAndFormatsDates, use-case part)
        Recall recall = Recall.builder(PaymentId.parse("00000000-0000-0000-0000-000000000501")).recallType("R-COR").build();
        when(loadRecalls.search(any())).thenReturn(List.of(recall));

        // WHEN
        List<Recall> actual = searchRecallsService.search(new RecallSearchQuery("r-cor", null, null, null, null, null, null, null));

        // THEN
        assertThat(actual).containsExactly(recall);
        verify(loadRecalls).search(any());
        verifyNoMoreInteractions(loadRecalls);
    }

    @Test
    void should_load_with_the_criteria_built_from_the_raw_query_and_ignore_the_limit() {
        // GIVEN
        RecallSearchQuery query = new RecallSearchQuery("r-cor", "00000000-0000-0000-0000-000000000401", "outbound",
                "ac04", "initiated", "2026-10-01T00:00:00", "2026-10-31T00:00:00", 5);
        ArgumentCaptor<RecallSearchCriteria> criteria = ArgumentCaptor.forClass(RecallSearchCriteria.class);

        // WHEN
        searchRecallsService.search(query);

        // THEN
        verify(loadRecalls).search(criteria.capture());
        assertThat(criteria.getValue())
                .returns(Optional.of(new CodeFilter("R-COR")), RecallSearchCriteria::recallType)
                .returns(Optional.of(PaymentId.parse("00000000-0000-0000-0000-000000000401")), RecallSearchCriteria::originalPaymentId)
                .returns(new SearchLimit(50), RecallSearchCriteria::limit);
        verifyNoMoreInteractions(loadRecalls);
    }

    @Test
    void should_not_load_anything_when_the_recall_type_is_unknown() {
        // GIVEN (remapped from PaymentServiceUnitTest.should_reject_unknown_recall_type_with_raw_input_in_message, interaction part)
        RecallSearchQuery query = new RecallSearchQuery(" nope ", null, null, null, null, null, null, null);

        // WHEN
        Throwable actual = catchThrowable(() -> searchRecallsService.search(query));

        // THEN
        assertThat(actual).isInstanceOf(UnknownRecallTypeException.class).hasMessage("Unknown recall type:  nope ");
        verifyNoInteractions(loadRecalls);
    }
}
