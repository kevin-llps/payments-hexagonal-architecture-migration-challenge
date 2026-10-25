package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;

import java.util.Set;

/**
 * Recall types accepted as a search filter. Ingested recalls are not checked.
 */
public final class RecallType {

    private static final Set<String> KNOWN_TYPES = Set.of("R-REJ", "R-COR", "R-UPD");

    private RecallType() {
    }

    /**
     * @return the normalized (trimmed, upper-case) recall type
     */
    public static String validateFilter(String raw) {
        String normalized = raw.trim().toUpperCase();
        if (!KNOWN_TYPES.contains(normalized)) {
            throw new InvalidSearchFilterException("Unknown recall type: " + raw);
        }
        return normalized;
    }
}
