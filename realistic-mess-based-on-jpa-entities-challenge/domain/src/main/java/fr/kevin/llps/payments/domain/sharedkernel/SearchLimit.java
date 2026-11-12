package fr.kevin.llps.payments.domain.sharedkernel;

/**
 * Maximum number of search results (BR-SHR-02, BR-SHR-03, BR-INB-06, BR-RCL-05).
 */
public record SearchLimit(int value) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    public SearchLimit {
        if (value < 0) {
            throw new NegativeSearchLimitException();
        }
    }

    /** Absent gives 50, above 200 gives 200, any other value is used as given. */
    public static SearchLimit capped(Integer requested) {
        if (requested == null) {
            return new SearchLimit(DEFAULT_LIMIT);
        }
        return new SearchLimit(Math.min(requested, MAX_LIMIT));
    }

    /** Absent gives 50, any other value is used as given. */
    public static SearchLimit uncapped(Integer requested) {
        if (requested == null) {
            return new SearchLimit(DEFAULT_LIMIT);
        }
        return new SearchLimit(requested);
    }

    /** Always 50, whatever was requested. */
    public static SearchLimit fixedDefault() {
        return new SearchLimit(DEFAULT_LIMIT);
    }
}
