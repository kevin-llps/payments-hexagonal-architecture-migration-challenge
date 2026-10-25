package fr.kevin.llps.payments.shared.domain;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Parses date filter text exactly like {@code java.sql.Date.valueOf}: {@code yyyy-[m]m-[d]d}, month 1-12, day 1-31,
 * an out-of-range day rolled over into the next month (2026-02-31 is March 3rd).
 */
public final class LenientDate {

    private static final int YEAR_LENGTH = 4;
    private static final int MAX_MONTH_OR_DAY_LENGTH = 2;
    private static final int MAX_MONTH = 12;
    private static final int MAX_DAY = 31;

    private LenientDate() {
    }

    /**
     * @return the parsed date, or empty where {@code Date.valueOf} would reject the text
     */
    public static Optional<LocalDate> parse(String text) {
        int firstDash = text.indexOf('-');
        int secondDash = text.indexOf('-', firstDash + 1);
        int length = text.length();
        if (firstDash <= 0 || secondDash <= 0 || secondDash >= length - 1) {
            return Optional.empty();
        }
        if (firstDash != YEAR_LENGTH
                || secondDash - firstDash <= 1 || secondDash - firstDash > MAX_MONTH_OR_DAY_LENGTH + 1
                || length - secondDash <= 1 || length - secondDash > MAX_MONTH_OR_DAY_LENGTH + 1) {
            return Optional.empty();
        }
        try {
            int year = Integer.parseInt(text, 0, firstDash, 10);
            int month = Integer.parseInt(text, firstDash + 1, secondDash, 10);
            int day = Integer.parseInt(text, secondDash + 1, length, 10);
            if (month < 1 || month > MAX_MONTH || day < 1 || day > MAX_DAY) {
                return Optional.empty();
            }
            return Optional.of(LocalDate.of(year, 1, 1).plusMonths(month - 1L).plusDays(day - 1L));
        } catch (NumberFormatException | DateTimeException e) {
            return Optional.empty();
        }
    }
}
