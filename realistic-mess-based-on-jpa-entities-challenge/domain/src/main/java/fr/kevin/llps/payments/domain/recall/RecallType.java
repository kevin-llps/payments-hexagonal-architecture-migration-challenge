package fr.kevin.llps.payments.domain.recall;

import java.util.Set;

/**
 * Recall type code. Open vocabulary on ingestion (BR-SHR-10); only the search filter is restricted (BR-RCL-01).
 */
public record RecallType(String value) {

    public static final Set<String> SEARCHABLE_TYPES = Set.of("R-REJ", "R-COR", "R-UPD");

    /** True when the code, trimmed and upper-cased, is one of the searchable types. */
    public boolean isSearchable() {
        return value != null && SEARCHABLE_TYPES.contains(value.trim().toUpperCase());
    }
}
