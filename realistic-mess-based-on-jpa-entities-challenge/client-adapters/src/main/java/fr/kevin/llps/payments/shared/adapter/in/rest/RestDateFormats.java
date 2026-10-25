package fr.kevin.llps.payments.shared.adapter.in.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date and timestamp rendering shared by every REST response.
 */
public final class RestDateFormats {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private RestDateFormats() {
    }

    public static String timestamp(LocalDateTime value) {
        return value == null ? null : TIMESTAMP.format(value);
    }

    public static String date(LocalDate value) {
        return value == null ? null : DATE.format(value);
    }
}
