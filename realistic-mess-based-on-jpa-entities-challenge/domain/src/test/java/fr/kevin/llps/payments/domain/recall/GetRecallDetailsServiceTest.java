package fr.kevin.llps.payments.domain.recall;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.recall.port.out.LoadRecalls;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRecallDetailsServiceTest {

    @Mock
    private LoadRecalls loadRecalls;

    @InjectMocks
    private GetRecallDetailsService getRecallDetailsService;

    @Test
    void should_return_the_recall_when_it_exists() {
        // GIVEN (remapped from PaymentServiceUnitTest.recallGetById_foundAndMissing, found case)
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000501");
        Recall recall = Recall.builder(id).build();
        when(loadRecalls.findById(id)).thenReturn(Optional.of(recall));

        // WHEN
        Optional<Recall> actual = getRecallDetailsService.getDetails(id);

        // THEN
        assertThat(actual).contains(recall);
        verify(loadRecalls).findById(id);
        verifyNoMoreInteractions(loadRecalls);
    }

    @Test
    void should_return_empty_when_the_recall_does_not_exist() {
        // GIVEN (remapped from PaymentServiceUnitTest.recallGetById_foundAndMissing, missing case; unknown UUID)
        PaymentId id = PaymentId.parse("11111111-1111-1111-1111-111111111111");
        when(loadRecalls.findById(id)).thenReturn(Optional.empty());

        // WHEN
        Optional<Recall> actual = getRecallDetailsService.getDetails(id);

        // THEN
        assertThat(actual).isEmpty();
        verify(loadRecalls).findById(id);
        verifyNoMoreInteractions(loadRecalls);
    }
}
