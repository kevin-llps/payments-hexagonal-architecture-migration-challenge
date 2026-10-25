package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.recall.port.in.RecordRecallCommand;
import fr.kevin.llps.payments.recall.port.out.RecallRecordRepository;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.MalformedValueException;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecordRecallServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-25T09:15:30.123456789Z"), ZoneOffset.UTC);

    @Mock
    private RecallRecordRepository repository;

    private RecordRecallService service;

    @BeforeEach
    void setUp() {
        service = new RecordRecallService(repository, CLOCK);
    }

    @Test
    void record_should_upsert_the_recall_stamped_now_with_millisecond_precision() {
        // GIVEN
        RecordRecallCommand command = command("00000000-0000-0000-0000-000000000905",
                "00000000-0000-0000-0000-000000000904");

        // WHEN
        service.record(command);

        // THEN
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 9, 15, 30, 123_000_000);
        verify(repository).upsert(new Recall(
                PaymentId.parse("00000000-0000-0000-0000-000000000905"), "RCL-1", "R-COR",
                new OriginalPaymentRef(PaymentId.parse("00000000-0000-0000-0000-000000000904"), "OUTBOUND"),
                "AC04", new BigDecimal("300.00"), "EUR", LocalDateTime.of(2026, 10, 17, 12, 0),
                LocalDate.of(2026, 10, 19), "INITIATED", new AuditTimestamps(now, now)));
    }

    // Regression check: BR-23 / BR-28 come from PaymentId.parse; the service must not upsert when either id is invalid.
    @ParameterizedTest(name = "[{index}] id=\"{0}\", originalPaymentId=\"{1}\"")
    @CsvSource({
            "'', 00000000-0000-0000-0000-000000000904",
            "not-a-uuid, 00000000-0000-0000-0000-000000000904",
            "00000000-0000-0000-0000-000000000905, ''",
            "00000000-0000-0000-0000-000000000905, not-a-uuid"
    })
    void record_should_reject_an_invalid_id_or_original_payment_id_without_upserting(String id, String originalPaymentId) {
        assertThatThrownBy(() -> service.record(command(id, originalPaymentId)))
                .isInstanceOf(MalformedValueException.class);
        verifyNoInteractions(repository);
    }

    private static RecordRecallCommand command(String id, String originalPaymentId) {
        return new RecordRecallCommand(id, "RCL-1", "R-COR", originalPaymentId, "OUTBOUND", "AC04",
                new BigDecimal("300.00"), "EUR", LocalDateTime.of(2026, 10, 17, 12, 0), LocalDate.of(2026, 10, 19),
                "INITIATED");
    }
}
