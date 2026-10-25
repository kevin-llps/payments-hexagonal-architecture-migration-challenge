package fr.kevin.llps.payments.shared.domain;

/**
 * Plausibility check applied to IBAN search filters (length only, no checksum).
 */
public final class IbanFilter {

    private static final int MIN_LENGTH = 15;
    private static final int MAX_LENGTH = 34;

    private IbanFilter() {
    }

    public static boolean isPlausible(String iban) {
        if (iban == null) {
            return false;
        }
        String cleaned = iban.replace(" ", "");
        return cleaned.length() >= MIN_LENGTH && cleaned.length() <= MAX_LENGTH;
    }
}
