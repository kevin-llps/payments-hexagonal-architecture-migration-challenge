package fr.kevin.llps.payments.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Generic helpers used everywhere in the codebase.
 */
public final class PaymentUtils {

    public static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public static final SimpleDateFormat DAY_FMT = new SimpleDateFormat("yyyy-MM-dd");
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;
    public static final BigDecimal DEFAULT_FEE = new BigDecimal("0.30");

    private PaymentUtils() {
    }

    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static boolean isValidIban(String iban) {
        if (iban == null) {
            return false;
        }
        String cleaned = iban.replace(" ", "");
        return cleaned.length() >= 15 && cleaned.length() <= 34;
    }

    public static Timestamp parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new Timestamp(FMT.parse(value.trim()).getTime());
        } catch (ParseException e) {
            // legacy behaviour: silently ignore unparsable dates
            return null;
        }
    }

    public static String now() {
        return FMT.format(new Date());
    }

    public static int limitOrDefault(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;
    }

    public static String likePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + value.trim().toUpperCase() + "%";
    }

    public static UUID randomId() {
        return UUID.randomUUID();
    }
}
