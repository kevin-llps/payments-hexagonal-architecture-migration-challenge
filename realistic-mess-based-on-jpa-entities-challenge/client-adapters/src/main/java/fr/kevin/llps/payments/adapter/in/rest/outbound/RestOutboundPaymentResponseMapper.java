package fr.kevin.llps.payments.adapter.in.rest.outbound;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import fr.kevin.llps.payments.domain.outbound.OutboundPayment;
import fr.kevin.llps.payments.domain.outbound.OutboundPaymentWithFee;

/**
 * Two intentionally different shapes (BR-OUT-09, BR-API-09): the search row in camelCase without audit timestamps,
 * and the details as the legacy snake_case {@link HashMap} (same 13 keys, so the same JSON key order, nulls kept).
 */
final class RestOutboundPaymentResponseMapper {

    OutboundPaymentResponse toSearchResponse(OutboundPaymentWithFee withFee) {
        OutboundPayment payment = withFee.payment();
        return new OutboundPaymentResponse(
                payment.id().value().toString(),
                payment.instructionId().orElse(null),
                payment.money().amount().orElse(null),
                payment.money().currency().orElse(null),
                payment.debtorIban().orElse(null),
                payment.debtorName().orElse(null),
                payment.destinationBic().orElse(null),
                payment.requestedExecutionDate().map(LocalDate::toString).orElse(null),
                payment.chargesType().orElse(null),
                payment.status().orElse(null),
                withFee.estimatedFee());
    }

    Map<String, Object> toDetailsResponse(OutboundPaymentWithFee withFee, RestOutboundPaymentDetailsFormatter formatter) {
        OutboundPayment payment = withFee.payment();
        Map<String, Object> response = new HashMap<>();
        response.put("id", payment.id().value().toString());
        response.put("instruction_id", payment.instructionId().orElse(null));
        response.put("amount", payment.money().amount().orElse(null));
        response.put("currency", payment.money().currency().orElse(null));
        response.put("debtor_iban", payment.debtorIban().orElse(null));
        response.put("debtor_name", payment.debtorName().orElse(null));
        response.put("destination_bic", payment.destinationBic().orElse(null));
        response.put("requested_execution_date", formatter.formatDate(payment.requestedExecutionDate().orElse(null)));
        response.put("charges_type", payment.chargesType().orElse(null));
        response.put("status", payment.status().orElse(null));
        response.put("created_at", formatter.formatTimestamp(payment.createdAt().orElse(null)));
        response.put("updated_at", formatter.formatTimestamp(payment.updatedAt().orElse(null)));
        response.put("estimatedFee", withFee.estimatedFee());
        return response;
    }
}
