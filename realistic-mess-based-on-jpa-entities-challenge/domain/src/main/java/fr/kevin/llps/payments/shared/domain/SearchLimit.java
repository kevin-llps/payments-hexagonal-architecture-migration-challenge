package fr.kevin.llps.payments.shared.domain;

/**
 * Maximum number of rows a search returns.
 */
public record SearchLimit(int value) {

    private static final int DEFAULT = 50;
    private static final int MAX = 200;

    /**
     * Default 50, capped at 200.
     */
    public static SearchLimit capped(Integer requested) {
        if (requested == null) {
            return new SearchLimit(DEFAULT);
        }
        if (requested > MAX) {
            return new SearchLimit(MAX);
        }
        return new SearchLimit(requested);
    }

    /**
     * Default 50, no cap.
     */
    public static SearchLimit uncapped(Integer requested) {
        if (requested == null) {
            return new SearchLimit(DEFAULT);
        }
        return new SearchLimit(requested);
    }

    /**
     * Always 50, whatever is requested.
     */
    public static SearchLimit fixed() {
        return new SearchLimit(DEFAULT);
    }
}
