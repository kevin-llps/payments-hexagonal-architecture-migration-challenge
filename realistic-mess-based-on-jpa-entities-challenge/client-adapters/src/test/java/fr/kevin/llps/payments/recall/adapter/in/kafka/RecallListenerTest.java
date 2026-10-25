package fr.kevin.llps.payments.recall.adapter.in.kafka;

import fr.kevin.llps.payments.recall.port.in.RecordRecallCommand;
import fr.kevin.llps.payments.recall.port.in.RecordRecallUseCase;
import fr.kevin.llps.payments.shared.domain.MalformedValueException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link RecallListener} message handling; carried over from the legacy KafkaListenerUnitTest.
 */
@ExtendWith(MockitoExtension.class)
class RecallListenerTest {

    @Mock
    private RecordRecallUseCase recordRecall;
    @InjectMocks
    private RecallListener recallListener;

    @ParameterizedTest(name = "[{index}] originalPaymentId fragment \"{0}\" drops the recall")
    @ValueSource(strings = {"", ",\"originalPaymentId\":\"not-a-uuid\""})
    void recall_onMessage_missingOrInvalidOriginalPaymentId_isDropped(String originalIdFragment) {
        // the use case rejects the reference (RecordRecallServiceTest); the listener must swallow the rejection
        doThrow(new MalformedValueException("Invalid payment id", null)).when(recordRecall).record(any());

        assertThatCode(() -> recallListener.onMessage("{\"id\":\"00000000-0000-0000-0000-000000000905\""
                + originalIdFragment + "}")).doesNotThrowAnyException();
        assertThat(captureRecall().originalPaymentId()).isIn("", "not-a-uuid");
    }

    @Test
    void recall_onMessage_dateOnlyInitiatedAt_defaultsToMidnight() {
        recallListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000905",
                 "originalPaymentId":"11111111-1111-1111-1111-111111111111","initiatedAt":"2026-10-17",
                 "deadline":"2026-10-19T00:00:00Z"}
                """);

        RecordRecallCommand recall = captureRecall();
        assertThat(recall.initiatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 0, 0, 0));
        assertThat(recall.deadline()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void recall_onMessage_persistsRow() {
        recallListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000905","recallReference":"RCL-1","recallType":"R-COR",
                 "originalPaymentId":"00000000-0000-0000-0000-000000000904","originalPaymentType":"OUTBOUND",
                 "reasonCode":"AC04","amount":300.00,"currency":"EUR","initiatedAt":"2026-10-17 12:00:00",
                 "deadline":"2026-10-19","status":"INITIATED"}
                """);

        RecordRecallCommand recall = captureRecall();
        assertThat(recall.recallReference()).isEqualTo("RCL-1");
        assertThat(recall.originalPaymentId()).isEqualTo("00000000-0000-0000-0000-000000000904");
        assertThat(recall.initiatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 12, 0, 0));
        assertThat(recall.deadline()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(recall.reasonCode()).isEqualTo("AC04");
    }

    @Test
    void recall_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> recallListener.onMessage("<>")).doesNotThrowAnyException();
        verify(recordRecall, never()).record(any());
    }

    private RecordRecallCommand captureRecall() {
        ArgumentCaptor<RecordRecallCommand> captor = ArgumentCaptor.forClass(RecordRecallCommand.class);
        verify(recordRecall).record(captor.capture());
        return captor.getValue();
    }

    @Test
    void listener_consumesItsTopicWithDedicatedConsumerGroup() throws Exception {
        KafkaListener annotation = RecallListener.class.getMethod("onMessage", String.class)
                .getAnnotation(KafkaListener.class);

        assertThat(annotation.topics()).containsExactly("pay.recall");
        assertThat(annotation.groupId()).isEqualTo("jpa-recall-group");
    }
}
