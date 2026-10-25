package fr.kevin.llps.payments.shared.domain;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Parses timestamp filter text exactly like {@code java.sql.Timestamp.valueOf}: {@code yyyy-[m]m-[d]d hh:mm:ss[.f...]},
 * month 1-12, day 1-31, out-of-range values rolled over (2026-02-31 is March 3rd, 25:00:00 is 01:00 the next day).
 */
public final class LenientTimestamp {

    private static final int YEAR_LENGTH = 4;
    private static final int MAX_MONTH_OR_DAY_LENGTH = 2;
    private static final int MAX_FRACTION_DIGITS = 9;
    private static final int MAX_MONTH = 12;
    private static final int MAX_DAY = 31;

    private LenientTimestamp() {
    }

    /**
     * @return the parsed date-time, or empty where {@code Timestamp.valueOf} would reject the text
     */
    public static Optional<LocalDateTime> parse(String text) {
        String s = text.trim();
        int space = s.indexOf(' ');
        if (space <= 0) {
            return Optional.empty();
        }
        String date = s.substring(0, space);
        String time = s.substring(space + 1);
        int firstDash = date.indexOf('-');
        int secondDash = date.indexOf('-', firstDash + 1);
        int firstColon = time.indexOf(':');
        int secondColon = time.indexOf(':', firstColon + 1);
        if (firstDash <= 0 || secondDash <= 0 || secondDash >= date.length() - 1) {
            return Optional.empty();
        }
        String year = date.substring(0, firstDash);
        String month = date.substring(firstDash + 1, secondDash);
        String day = date.substring(secondDash + 1);
        if (year.length() != YEAR_LENGTH
                || month.isEmpty() || month.length() > MAX_MONTH_OR_DAY_LENGTH
                || day.isEmpty() || day.length() > MAX_MONTH_OR_DAY_LENGTH) {
            return Optional.empty();
        }
        if (firstColon <= 0 || secondColon <= 0 || secondColon >= time.length() - 1) {
            return Optional.empty();
        }
        int period = time.indexOf('.', secondColon + 1);
        try {
            String seconds;
            int nanos = 0;
            if (period > 0 && period < time.length() - 1) {
                seconds = time.substring(secondColon + 1, period);
                String fraction = time.substring(period + 1);
                if (fraction.length() > MAX_FRACTION_DIGITS || !Character.isDigit(fraction.charAt(0))) {
                    return Optional.empty();
                }
                nanos = Integer.parseInt(fraction) * (int) Math.pow(10, MAX_FRACTION_DIGITS - fraction.length());
            } else if (period > 0) {
                return Optional.empty();
            } else {
                seconds = time.substring(secondColon + 1);
            }
            int monthValue = Integer.parseInt(month);
            int dayValue = Integer.parseInt(day);
            if (monthValue < 1 || monthValue > MAX_MONTH || dayValue < 1 || dayValue > MAX_DAY) {
                return Optional.empty();
            }
            return Optional.of(LocalDateTime.of(Integer.parseInt(year), 1, 1, 0, 0)
                    .plusMonths(monthValue - 1L)
                    .plusDays(dayValue - 1L)
                    .plusHours(Integer.parseInt(time.substring(0, firstColon)))
                    .plusMinutes(Integer.parseInt(time.substring(firstColon + 1, secondColon)))
                    .plusSeconds(Integer.parseInt(seconds))
                    .plusNanos(nanos));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
