package fr.kevin.llps.payments.domain.recall;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.recall.command.PersistRecallCommand;
import fr.kevin.llps.payments.domain.recall.port.out.SaveRecall;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PersistRecallServiceTest {

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));

    @Mock
    private SaveRecall saveRecall;

    private PersistRecallService persistRecallService;

    @BeforeEach
    void createService() {
        persistRecallService = new PersistRecallService(saveRecall, fixedClock);
    }

    @Test
    void should_save_the_event_as_a_full_recall_with_consumer_time_audit_timestamps() {
        // GIVEN
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000905");
        PaymentId original = PaymentId.parse("00000000-0000-0000-0000-000000000904");
        PersistRecallCommand command = new PersistRecallCommand(id, "RCL-1", "R-COR", original, "OUTBOUND", "AC04",
                new BigDecimal("300.00"), "EUR", LocalDateTime.of(2026, 10, 17, 12, 0), LocalDate.of(2026, 10, 19),
                "INITIATED");
        ArgumentCaptor<Recall> saved = ArgumentCaptor.forClass(Recall.class);

        // WHEN
        persistRecallService.persist(command);

        // THEN
        verify(saveRecall).save(saved.capture());
        verifyNoMoreInteractions(saveRecall);
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        assertThat(saved.getValue()).isEqualTo(Recall.builder(id)
                .recallReference("RCL-1")
                .recallType("R-COR")
                .originalPayment(OriginalPaymentReference.of(original, "OUTBOUND"))
                .reasonCode("AC04")
                .money(Money.of(new BigDecimal("300.00"), "EUR"))
                .initiatedAt(LocalDateTime.of(2026, 10, 17, 12, 0))
                .deadline(LocalDate.of(2026, 10, 19))
                .status("INITIATED")
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build());
    }
}
