package fr.kevin.llps.payments.adapter.in.rest.recall;

import java.time.format.DateTimeFormatter;

import fr.kevin.llps.payments.domain.recall.Recall;
import fr.kevin.llps.payments.domain.sharedkernel.ApiDateTimeFormat;

/**
 * Renders a recall like the legacy service did: {@code initiatedAt} as {@code yyyy-MM-dd'T'HH:mm:ss},
 * {@code deadline} as {@code yyyy-MM-dd}, absent values as {@code null} (BR-RCL-08, BR-SHR-05).
 */
final class RestRecallResponseMapper {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(ApiDateTimeFormat.TIMESTAMP_PATTERN);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(ApiDateTimeFormat.DATE_PATTERN);

    RecallResponse toResponse(Recall recall) {
        return new RecallResponse(
                recall.id().value().toString(),
                recall.recallReference().orElse(null),
                recall.recallType().orElse(null),
                recall.originalPayment().paymentId().map(id -> id.value().toString()).orElse(null),
                recall.originalPayment().paymentType().orElse(null),
                recall.reasonCode().orElse(null),
                recall.money().amount().orElse(null),
                recall.money().currency().orElse(null),
                recall.initiatedAt().map(TIMESTAMP_FORMAT::format).orElse(null),
                recall.deadline().map(DATE_FORMAT::format).orElse(null),
                recall.status().orElse(null));
    }
}
