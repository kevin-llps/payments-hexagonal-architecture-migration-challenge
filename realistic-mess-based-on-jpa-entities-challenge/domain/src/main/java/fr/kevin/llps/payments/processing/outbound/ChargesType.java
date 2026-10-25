package fr.kevin.llps.payments.processing.outbound;

/**
 * Who bears the transfer charges, as received (codes are compared exactly; unknown or empty codes are kept).
 */
public record ChargesType(String code) {

    public boolean isOur() {
        return "OUR".equals(code);
    }

    public boolean isSha() {
        return "SHA".equals(code);
    }

    public boolean isBen() {
        return "BEN".equals(code);
    }
}
