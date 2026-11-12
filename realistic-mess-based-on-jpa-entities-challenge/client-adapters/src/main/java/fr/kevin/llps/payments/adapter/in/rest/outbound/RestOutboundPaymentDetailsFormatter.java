package fr.kevin.llps.payments.adapter.in.rest.outbound;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import fr.kevin.llps.payments.domain.sharedkernel.ApiDateTimeFormat;

/**
 * Renders the outbound details dates exactly as the legacy Jackson output of {@code java.sql.Timestamp} /
 * {@code java.sql.Date} (BR-OUT-09, N-02): a stored wall-clock time is read in the application zone (DST gap shifted
 * forward, DST overlap on the later offset, as {@code Timestamp.valueOf}), shown in UTC with milliseconds and a literal
 * {@code +00:00}; dates as {@code yyyy-MM-dd}. Absent values stay {@code null}.
 */
final class RestOutboundPaymentDetailsFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final String UTC_SUFFIX = "+00:00";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(ApiDateTimeFormat.DATE_PATTERN);

    private final ZoneId zone;

    RestOutboundPaymentDetailsFormatter(ZoneId zone) {
        this.zone = zone;
    }

    String formatTimestamp(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        LocalDateTime utc = value.atZone(zone)
                .withLaterOffsetAtOverlap()
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        return TIMESTAMP_FORMAT.format(utc) + UTC_SUFFIX;
    }

    String formatDate(LocalDate value) {
        return value == null ? null : DATE_FORMAT.format(value);
    }
}
