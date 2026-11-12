package fr.kevin.llps.payments.domain.sharedkernel;

import java.util.Optional;

/**
 * Code-like equality filter: trimmed and upper-cased, matched exactly (BR-SHR-08).
 */
public record CodeFilter(String value) {

    public static Optional<CodeFilter> from(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        // Default-locale upper-casing, identical to the legacy repositories.
        return Optional.of(new CodeFilter(raw.trim().toUpperCase()));
    }
}
