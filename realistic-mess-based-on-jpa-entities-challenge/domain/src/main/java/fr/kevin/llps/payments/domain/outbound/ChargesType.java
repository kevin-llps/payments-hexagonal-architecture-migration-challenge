package fr.kevin.llps.payments.domain.outbound;

/**
 * Charges bearer code of an outbound payment. Open vocabulary: only {@code OUR}, {@code SHA} and {@code BEN} carry
 * fee behaviour, compared exactly and case-sensitively (BR-OUT-06, BR-OUT-07, OQ-01).
 */
public record ChargesType(String value) {

    public static final String OUR = "OUR";
    public static final String SHA = "SHA";
    public static final String BEN = "BEN";

    public boolean is(String code) {
        return code.equals(value);
    }
}
