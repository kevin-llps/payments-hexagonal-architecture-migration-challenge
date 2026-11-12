package fr.kevin.llps.payments.adapter.in.rest.suspended;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import fr.kevin.llps.payments.domain.sharedkernel.ApiDateTimeFormat;
import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;

/**
 * Renders a suspended payment like the legacy service did: timestamps as {@code yyyy-MM-dd'T'HH:mm:ss}, absent values
 * as {@code null} (BR-SHR-05, BR-SUS-06).
 */
final class RestSuspendedPaymentResponseMapper {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(ApiDateTimeFormat.TIMESTAMP_PATTERN);

    SuspendedPaymentResponse toResponse(SuspendedPayment payment) {
        return new SuspendedPaymentResponse(
                payment.id().value().toString(),
                payment.money().amount().orElse(null),
                payment.money().currency().orElse(null),
                payment.debtorIban().orElse(null),
                payment.debtorName().orElse(null),
                payment.creditorIban().orElse(null),
                payment.creditorName().orElse(null),
                payment.holdReason().orElse(null),
                payment.complianceCaseRef().orElse(null),
                payment.reviewStatus().orElse(null),
                payment.heldAt().map(RestSuspendedPaymentResponseMapper::format).orElse(null),
                payment.releasedAt().map(RestSuspendedPaymentResponseMapper::format).orElse(null));
    }

    private static String format(LocalDateTime value) {
        return TIMESTAMP_FORMAT.format(value);
    }
}
