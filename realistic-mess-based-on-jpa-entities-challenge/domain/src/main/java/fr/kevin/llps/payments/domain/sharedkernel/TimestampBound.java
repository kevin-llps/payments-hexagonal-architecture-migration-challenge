package fr.kevin.llps.payments.domain.sharedkernel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Inclusive timestamp bound of a search filter, parsed with one of the two legacy grammars.
 *
 * <ul>
 *   <li>{@link #lenient(String, LocalTime)}: the JDBC timestamp grammar {@code yyyy-[m]m-[d]d hh:mm:ss[.f...]}, with
 *       {@code T} accepted as separator and a ten-character date completed with a given time; unparsable input gives
 *       no bound (BR-INS-06/07, BR-RCL-04).</li>
 *   <li>{@link #strictApiFormat(String)}: the lenient {@code yyyy-MM-dd'T'HH:mm:ss} API grammar; unparsable input
 *       gives an unsatisfiable bound that matches nothing (BR-SHR-06, BR-SUS-03/04).</li>
 * </ul>
 *
 * <p>Both grammars roll out-of-range fields over, as the legacy lenient calendar does. Values are wall-clock times
 * without zone: resolving a daylight-saving gap or overlap is left to the persistence adapter, exactly as the legacy
 * {@code Timestamp} construction did.</p>
 */
public final class TimestampBound {

    private static final TimestampBound ABSENT = new TimestampBound(null, false);
    private static final TimestampBound UNSATISFIABLE = new TimestampBound(null, true);

    private static final int DATE_ONLY_LENGTH = 10;
    private static final int YEAR_LENGTH = 4;
    private static final int MONTH_LENGTH = 2;
    private static final int DAY_LENGTH = 2;
    private static final int MAX_MONTH = 12;
    private static final int MAX_DAY = 31;
    private static final int MAX_FRACTION_DIGITS = 9;
    private static final int RADIX = 10;

    private final LocalDateTime value;
    private final boolean unsatisfiable;

    private TimestampBound(LocalDateTime value, boolean unsatisfiable) {
        this.value = value;
        this.unsatisfiable = unsatisfiable;
    }

    public static TimestampBound absent() {
        return ABSENT;
    }

    public static TimestampBound lenient(String raw, LocalTime dateOnlyTime) {
        Objects.requireNonNull(dateOnlyTime, "dateOnlyTime");
        if (raw == null || raw.isBlank()) {
            return ABSENT;
        }
        String text = raw.trim().replace('T', ' ');
        if (text.length() == DATE_ONLY_LENGTH) {
            text = text + " " + String.format("%02d:%02d:%02d",
                    dateOnlyTime.getHour(), dateOnlyTime.getMinute(), dateOnlyTime.getSecond());
        }
        return parseJdbcTimestamp(text)
                .map(parsed -> new TimestampBound(parsed, false))
                .orElse(ABSENT);
    }

    public static TimestampBound strictApiFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            return ABSENT;
        }
        return new ApiFormatParser(raw.trim()).parse()
                .map(parsed -> new TimestampBound(parsed, false))
                .orElse(UNSATISFIABLE);
    }

    public Optional<LocalDateTime> value() {
        return Optional.ofNullable(value);
    }

    public boolean isUnsatisfiable() {
        return unsatisfiable;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TimestampBound bound
                && unsatisfiable == bound.unsatisfiable
                && Objects.equals(value, bound.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unsatisfiable);
    }

    @Override
    public String toString() {
        return "TimestampBound[value=" + value + ", unsatisfiable=" + unsatisfiable + "]";
    }

    private static Optional<LocalDateTime> parseJdbcTimestamp(String raw) {
        String text = raw.trim();
        int dividingSpace = text.indexOf(' ');
        if (dividingSpace < 0) {
            return Optional.empty();
        }
        int firstDash = text.indexOf('-');
        int secondDash = text.indexOf('-', firstDash + 1);
        int firstColon = text.indexOf(':', dividingSpace + 1);
        int secondColon = text.indexOf(':', firstColon + 1);
        int period = text.indexOf('.', secondColon + 1);
        int length = text.length();
        if (!hasJdbcDateShape(firstDash, secondDash, dividingSpace)
                || !(firstColon > 0 && secondColon > 0 && secondColon < length - 1)) {
            return Optional.empty();
        }
        try {
            int year = Integer.parseInt(text, 0, firstDash, RADIX);
            int month = Integer.parseInt(text, firstDash + 1, secondDash, RADIX);
            int day = Integer.parseInt(text, secondDash + 1, dividingSpace, RADIX);
            if (month < 1 || month > MAX_MONTH || day < 1 || day > MAX_DAY) {
                return Optional.empty();
            }
            int hour = Integer.parseInt(text, dividingSpace + 1, firstColon, RADIX);
            int minute = Integer.parseInt(text, firstColon + 1, secondColon, RADIX);
            int second;
            int nanos = 0;
            if (period > 0 && period < length - 1) {
                second = Integer.parseInt(text, secondColon + 1, period, RADIX);
                int fractionDigits = length - (period + 1);
                if (fractionDigits > MAX_FRACTION_DIGITS || !Character.isDigit(text.charAt(period + 1))) {
                    return Optional.empty();
                }
                nanos = Integer.parseInt(text, period + 1, length, RADIX);
                for (int digits = fractionDigits; digits < MAX_FRACTION_DIGITS; digits++) {
                    nanos *= RADIX;
                }
            } else if (period > 0) {
                return Optional.empty();
            } else {
                second = Integer.parseInt(text, secondColon + 1, length, RADIX);
            }
            return Optional.of(LocalDate.of(year, month, 1).atStartOfDay()
                    .plusDays(day - 1L)
                    .plusHours(hour)
                    .plusMinutes(minute)
                    .plusSeconds(second)
                    .plusNanos(nanos));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static boolean hasJdbcDateShape(int firstDash, int secondDash, int dividingSpace) {
        return firstDash > 0 && secondDash > 0 && secondDash < dividingSpace - 1
                && firstDash == YEAR_LENGTH
                && secondDash - firstDash > 1 && secondDash - firstDash <= MONTH_LENGTH + 1
                && dividingSpace - secondDash > 1 && dividingSpace - secondDash <= DAY_LENGTH + 1;
    }

    /**
     * Reproduces the legacy lenient {@code SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")} parsing: spaces or tabs are
     * skipped before each number, numbers take an optional minus sign, any number of digits and an optional
     * {@code E} exponent, literals must match exactly, and the text after the seconds is ignored.
     */
    private static final class ApiFormatParser {

        private static final char EXPONENT = 'E';
        private static final char MINUS = '-';

        private final String text;
        private int position;

        private ApiFormatParser(String text) {
            this.text = text;
        }

        private Optional<LocalDateTime> parse() {
            Optional<Integer> year = number();
            Optional<Integer> month = year.flatMap(ignored -> literal('-')).flatMap(ignored -> number());
            Optional<Integer> day = month.flatMap(ignored -> literal('-')).flatMap(ignored -> number());
            Optional<Integer> hour = day.flatMap(ignored -> literal('T')).flatMap(ignored -> number());
            Optional<Integer> minute = hour.flatMap(ignored -> literal(':')).flatMap(ignored -> number());
            Optional<Integer> second = minute.flatMap(ignored -> literal(':')).flatMap(ignored -> number());
            if (second.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(LocalDate.of(year.get(), 1, 1).atStartOfDay()
                    .plusMonths(month.get() - 1L)
                    .plusDays(day.get() - 1L)
                    .plusHours(hour.get())
                    .plusMinutes(minute.get())
                    .plusSeconds(second.get()));
        }

        private Optional<Boolean> literal(char expected) {
            if (position < text.length() && text.charAt(position) == expected) {
                position++;
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }

        private Optional<Integer> number() {
            while (position < text.length() && (text.charAt(position) == ' ' || text.charAt(position) == '\t')) {
                position++;
            }
            int start = position;
            boolean negative = position < text.length() && text.charAt(position) == MINUS;
            if (negative) {
                position++;
            }
            String mantissa = digits();
            if (mantissa.isEmpty()) {
                position = start;
                return Optional.empty();
            }
            BigDecimal number = new BigDecimal(new BigInteger(mantissa)).scaleByPowerOfTen(exponent());
            return Optional.of(toLegacyInt(negative ? number.negate() : number));
        }

        private int exponent() {
            if (position >= text.length() || text.charAt(position) != EXPONENT) {
                return 0;
            }
            int exponentStart = position;
            position++;
            boolean negative = position < text.length() && text.charAt(position) == MINUS;
            if (negative) {
                position++;
            }
            String exponentDigits = digits();
            if (exponentDigits.isEmpty()) {
                position = exponentStart;
                return 0;
            }
            int exponent = Integer.parseInt(exponentDigits);
            return negative ? -exponent : exponent;
        }

        private String digits() {
            StringBuilder digits = new StringBuilder();
            while (position < text.length() && Character.digit(text.charAt(position), RADIX) >= 0) {
                digits.append(Character.digit(text.charAt(position), RADIX));
                position++;
            }
            return digits.toString();
        }

        private static int toLegacyInt(BigDecimal number) {
            if (number.stripTrailingZeros().scale() <= 0 && number.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0
                    && number.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0) {
                return (int) number.longValueExact();
            }
            return (int) number.doubleValue();
        }
    }
}
