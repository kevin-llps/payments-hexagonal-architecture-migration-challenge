package fr.kevin.llps.payments.domain.sharedkernel;

/**
 * Raw IBAN; only its length is checked, never enforced (BR-SHR-04).
 */
public record Iban(String value) {

    public static final int MIN_LENGTH = 15;
    public static final int MAX_LENGTH = 34;

    public static Iban of(String raw) {
        return new Iban(raw);
    }

    /** Non-null and 15..34 characters once spaces (and only spaces) are removed. */
    public boolean hasValidLength() {
        if (value == null) {
            return false;
        }
        int length = value.replace(" ", "").length();
        return length >= MIN_LENGTH && length <= MAX_LENGTH;
    }
}
