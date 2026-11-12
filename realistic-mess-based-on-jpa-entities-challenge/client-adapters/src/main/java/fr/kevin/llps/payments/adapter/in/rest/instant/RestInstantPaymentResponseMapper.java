package fr.kevin.llps.payments.adapter.in.rest.instant;

import java.time.format.DateTimeFormatter;

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.sharedkernel.ApiDateTimeFormat;

/**
 * Renders an instant payment like the legacy repository did: {@code executionDatetime} as
 * {@code yyyy-MM-dd'T'HH:mm:ss}, absent values as {@code null} (BR-INS-09, BR-SHR-05).
 */
final class RestInstantPaymentResponseMapper {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(ApiDateTimeFormat.TIMESTAMP_PATTERN);

    InstantPaymentResponse toResponse(InstantPayment payment) {
        return new InstantPaymentResponse(
                payment.id().value().toString(),
                payment.endToEndId().orElse(null),
                payment.uetr().orElse(null),
                payment.money().amount().orElse(null),
                payment.money().currency().orElse(null),
                payment.debtorIban().orElse(null),
                payment.debtorName().orElse(null),
                payment.creditorIban().orElse(null),
                payment.creditorName().orElse(null),
                payment.executionDatetime().map(TIMESTAMP_FORMAT::format).orElse(null),
                payment.status().orElse(null),
                payment.scheme().orElse(null));
    }
}
