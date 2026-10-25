package fr.kevin.llps.payments.compliancehold;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Parses {@code yyyy-MM-dd'T'HH:mm:ss} the way a lenient {@code SimpleDateFormat} does: fields of any length,
 * out-of-range or negative values rolled over, text after the seconds ignored. Not reproduced: negative years
 * (the legacy format maps them to BC dates) and fields beyond {@code int} range (rejected here).
 */
final class LenientIsoTimestamp {

    private static final char[] SEPARATORS = {'-', '-', 'T', ':', ':'};
    private static final int FIELD_COUNT = SEPARATORS.length + 1;

    private LenientIsoTimestamp() {
    }

    /**
     * @return the parsed date-time, or empty where the lenient {@code SimpleDateFormat} would reject the text
     */
    static Optional<LocalDateTime> parse(String text) {
        int[] fields = new int[FIELD_COUNT];
        int position = 0;
        for (int i = 0; i < FIELD_COUNT; i++) {
            if (i > 0) {
                if (position >= text.length() || text.charAt(position) != SEPARATORS[i - 1]) {
                    return Optional.empty();
                }
                position++;
            }
            int start = position;
            if (position < text.length() && text.charAt(position) == '-') {
                position++;
            }
            int digitsStart = position;
            while (position < text.length() && Character.isDigit(text.charAt(position))) {
                position++;
            }
            if (position == digitsStart) {
                return Optional.empty();
            }
            try {
                fields[i] = Integer.parseInt(text.substring(start, position));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        try {
            return Optional.of(LocalDateTime.of(fields[0], 1, 1, 0, 0)
                    .plusMonths(fields[1] - 1L)
                    .plusDays(fields[2] - 1L)
                    .plusHours(fields[3])
                    .plusMinutes(fields[4])
                    .plusSeconds(fields[5]));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }
}
