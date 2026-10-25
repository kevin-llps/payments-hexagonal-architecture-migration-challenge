package fr.kevin.llps.payments.processing.instant.adapter.in.kafka;

import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.processing.instant.RecordInstantPaymentService;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentRecordRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link InstantPaymentListener} message handling; carried over from the legacy KafkaListenerUnitTest.
 */
@ExtendWith(MockitoExtension.class)
class InstantPaymentListenerTest {

    @Mock
    private InstantPaymentRecordRepository instantRepository;

    // real use case: scheme default and audit timestamps are part of what the instant listener tests assert
    private InstantPaymentListener instantListener;

    @BeforeEach
    void wireInstantListener() {
        instantListener = new InstantPaymentListener(
                new RecordInstantPaymentService(instantRepository, Clock.systemDefaultZone()));
    }

    @Test
    void instant_onMessage_persistsRow() {
        instantListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000901","endToEndId":"E2E-1","uetr":"uetr-1",
                 "amount":42.00,"currency":"EUR","debtorIban":"DE89","debtorName":"ACME",
                 "creditorIban":"FR76","creditorName":"Dupont","executionDateTime":"2026-10-17 10:00:00",
                 "status":"COMPLETED","scheme":"SCT INST"}
                """);

        InstantPayment payment = captureInstant();
        assertThat(payment.id()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000901"));
        assertThat(payment.endToEndId()).isEqualTo("E2E-1");
        assertThat(payment.amount().amount()).isEqualByComparingTo("42.00");
        assertThat(payment.executionDatetime()).isEqualTo(LocalDateTime.of(2026, 10, 17, 10, 0, 0));
        assertThat(payment.scheme()).isEqualTo("SCT INST");
        assertThat(payment.audit().createdAt()).isNotNull();
    }

    @Test
    void instant_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> instantListener.onMessage("not json")).doesNotThrowAnyException();
        verify(instantRepository, never()).upsert(any());
    }

    @ParameterizedTest(name = "[{index}] scheme fragment \"{0}\" defaults to SCT INST")
    @ValueSource(strings = {"", ",\"scheme\":null"})
    void instant_onMessage_missingOrNullScheme_defaultsToSctInst(String schemeFragment) {
        instantListener.onMessage("{\"id\":\"00000000-0000-0000-0000-000000000901\",\"amount\":1.00"
                + schemeFragment + "}");

        assertThat(captureInstant().scheme()).isEqualTo("SCT INST");
    }

    @ParameterizedTest(name = "[{index}] executionDateTime \"{0}\" -> {1}")
    @CsvSource({
            "2026-10-17T10:00:00Z, 2026-10-17T10:00:00",
            "2026-10-17T10:00:00, 2026-10-17T10:00:00",
            "2026-10-17, 2026-10-17T00:00:00"
    })
    void instant_onMessage_executionDateTime_stripsTAndZWithoutTimezoneConversion(String raw, LocalDateTime expected) {
        instantListener.onMessage("{\"id\":\"00000000-0000-0000-0000-000000000901\",\"amount\":1.00,"
                + "\"executionDateTime\":\"" + raw + "\"}");

        assertThat(captureInstant().executionDatetime()).isEqualTo(expected);
    }

    @Test
    void instant_onMessage_missingStringFieldsAreEmpty_nullStringFieldsAreLiteralNull_missingAmountIsZero() {
        instantListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000901","debtorName":null}
                """);

        InstantPayment payment = captureInstant();
        assertThat(payment.endToEndId()).isEmpty();
        assertThat(payment.status()).isEmpty();
        assertThat(payment.debtorName()).isEqualTo("null");
        assertThat(payment.amount().amount()).isEqualByComparingTo("0");
        assertThat(payment.executionDatetime()).isNull();
    }

    @Test
    void instant_onMessage_setsCreatedAtAndUpdatedAtToIngestionTime() {
        long before = System.currentTimeMillis();

        instantListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000901","amount":1.00}
                """);

        InstantPayment payment = captureInstant();
        long after = System.currentTimeMillis();
        assertThat(Timestamp.valueOf(payment.audit().createdAt()).getTime()).isBetween(before, after);
        assertThat(Timestamp.valueOf(payment.audit().updatedAt()).getTime()).isBetween(before, after);
    }

    @ParameterizedTest(name = "[{index}] id fragment \"{0}\" drops the message")
    @ValueSource(strings = {"\"id\":\"not-a-uuid\",", ""})
    void instant_onMessage_invalidOrMissingId_isDropped(String idFragment) {
        assertThatCode(() -> instantListener.onMessage("{" + idFragment + "\"amount\":1.00}"))
                .doesNotThrowAnyException();
        verify(instantRepository, never()).upsert(any());
    }

    private InstantPayment captureInstant() {
        ArgumentCaptor<InstantPayment> captor = ArgumentCaptor.forClass(InstantPayment.class);
        verify(instantRepository).upsert(captor.capture());
        return captor.getValue();
    }

    @Test
    void listener_consumesItsTopicWithDedicatedConsumerGroup() throws Exception {
        KafkaListener annotation = InstantPaymentListener.class.getMethod("onMessage", String.class)
                .getAnnotation(KafkaListener.class);

        assertThat(annotation.topics()).containsExactly("pay.instant");
        assertThat(annotation.groupId()).isEqualTo("jpa-instant-group");
    }
}
