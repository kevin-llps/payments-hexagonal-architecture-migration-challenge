package fr.kevin.llps.payments.domain.sharedkernel;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Inclusive date bound parsed with the legacy JDBC date grammar {@code yyyy-[m]m-[d]d} (BR-INB-04, BR-OUT-04,
 * BR-API-07): a four-character year, a one- or two-character month in 1..12 and day in 1..31, the day rolling over
 * the end of the month.
 */
public record IsoDateBound(LocalDate value) {

    private static final int YEAR_LENGTH = 4;
    private static final int MONTH_LENGTH = 2;
    private static final int DAY_LENGTH = 2;
    private static final int MAX_MONTH = 12;
    private static final int MAX_DAY = 31;
    private static final int RADIX = 10;

    public IsoDateBound {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Blank gives no bound. A malformed date throws {@link MalformedDateException}: without message for a shape or
     * range error, with the {@link NumberFormatException} message for a non-numeric field.
     */
    public static Optional<IsoDateBound> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String text = raw.trim();
        int firstDash = text.indexOf('-');
        int secondDash = text.indexOf('-', firstDash + 1);
        int length = text.length();
        if (!hasDateShape(firstDash, secondDash, length)) {
            throw new MalformedDateException(null);
        }
        int year = parseField(text, 0, firstDash);
        int month = parseField(text, firstDash + 1, secondDash);
        int day = parseField(text, secondDash + 1, length);
        if (month < 1 || month > MAX_MONTH || day < 1 || day > MAX_DAY) {
            throw new MalformedDateException(null);
        }
        return Optional.of(new IsoDateBound(LocalDate.of(year, month, 1).plusDays(day - 1L)));
    }

    private static boolean hasDateShape(int firstDash, int secondDash, int length) {
        return firstDash > 0 && secondDash > 0 && secondDash < length - 1
                && firstDash == YEAR_LENGTH
                && secondDash - firstDash > 1 && secondDash - firstDash <= MONTH_LENGTH + 1
                && length - secondDash > 1 && length - secondDash <= DAY_LENGTH + 1;
    }

    private static int parseField(String text, int begin, int end) {
        try {
            return Integer.parseInt(text, begin, end, RADIX);
        } catch (NumberFormatException e) {
            throw new MalformedDateException(e.getMessage());
        }
    }
}
