package fr.kevin.llps.payments.adapter.out.persistence.suspended;

import java.time.LocalDateTime;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;

import static org.assertj.core.api.Assertions.assertThat;

class JpaSuspendedPaymentMapperTest {

    private TimeZone originalTimeZone;
    private final JpaSuspendedPaymentMapper mapper = new JpaSuspendedPaymentMapper();

    @BeforeEach
    void useParisTimeZone() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
    }

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @ParameterizedTest(name = "{0}: {1} is bound as epoch millis {2}")
    @MethodSource("heldAtCases")
    void should_bind_the_wall_clock_time_like_the_legacy_timestamp_in_europe_paris(
            String testCase, LocalDateTime heldAt, long expectedEpochMillis) {
        // GIVEN (F-03: values observed from the legacy Timestamp.valueOf in Europe/Paris)
        SuspendedPayment payment = SuspendedPayment.builder(PaymentId.of(UUID.fromString("00000000-0000-0000-0000-000000000611")))
                .heldAt(heldAt)
                .build();

        // WHEN
        fr.kevin.llps.payments.entity.SuspendedPayment actual = mapper.toEntity(payment);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.heldAt.getTime()).isEqualTo(expectedEpochMillis);
    }

    static Stream<Arguments> heldAtCases() {
        return Stream.of(
                Arguments.of("regular time outside DST transitions", LocalDateTime.of(2026, 10, 14, 8, 0), 1791957600000L),
                Arguments.of("DST gap shifts forward like the legacy timestamp", LocalDateTime.of(2026, 3, 29, 2, 30), 1774747800000L),
                Arguments.of("DST overlap resolves to standard time like the legacy timestamp", LocalDateTime.of(2026, 10, 25, 2, 30), 1792891800000L)
        );
    }
}
