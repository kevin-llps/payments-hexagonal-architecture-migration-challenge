package fr.kevin.llps.payments.adapter.in.rest.inbound;

import java.time.format.DateTimeFormatter;

import fr.kevin.llps.payments.domain.inbound.InboundPayment;
import fr.kevin.llps.payments.domain.inbound.InboundPaymentView;
import fr.kevin.llps.payments.domain.sharedkernel.ApiDateTimeFormat;

/**
 * Renders an inbound payment like the legacy code did: dates as {@code yyyy-MM-dd}, absent values as {@code null},
 * and {@code ageInDays} as given by the use case (BR-INB-07, BR-SHR-05).
 */
final class RestInboundPaymentResponseMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(ApiDateTimeFormat.DATE_PATTERN);

    InboundPaymentResponse toResponse(InboundPaymentView view) {
        InboundPayment payment = view.payment();
        return new InboundPaymentResponse(
                payment.id().value().toString(),
                payment.creditTransferRef().orElse(null),
                payment.money().amount().orElse(null),
                payment.money().currency().orElse(null),
                payment.creditorIban().orElse(null),
                payment.creditorName().orElse(null),
                payment.originBic().orElse(null),
                payment.valueDate().map(DATE_FORMAT::format).orElse(null),
                payment.settlementDate().map(DATE_FORMAT::format).orElse(null),
                payment.status().orElse(null),
                view.ageInDays().orElse(null));
    }
}
