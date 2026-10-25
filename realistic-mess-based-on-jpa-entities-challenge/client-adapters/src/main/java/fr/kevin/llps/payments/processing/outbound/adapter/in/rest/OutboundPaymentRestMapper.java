package fr.kevin.llps.payments.processing.outbound.adapter.in.rest;

import fr.kevin.llps.payments.processing.outbound.OutboundPayment;
import fr.kevin.llps.payments.processing.outbound.adapter.in.rest.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.processing.outbound.port.in.PricedOutboundPayment;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static fr.kevin.llps.payments.shared.adapter.in.rest.RestDateFormats.date;

final class OutboundPaymentRestMapper {

    private OutboundPaymentRestMapper() {
    }

    static OutboundPaymentResponse toResponse(PricedOutboundPayment priced) {
        OutboundPayment payment = priced.payment();
        OutboundPaymentResponse r = new OutboundPaymentResponse();
        r.id = payment.id().value().toString();
        r.instructionId = payment.instructionId();
        r.amount = payment.amount().amount();
        r.currency = payment.amount().currency();
        r.debtorIban = payment.debtorIban();
        r.debtorName = payment.debtorName();
        r.destinationBic = payment.destinationBic();
        r.requestedExecutionDate = date(payment.requestedExecutionDate());
        r.chargesType = payment.chargesType().code();
        r.status = payment.status();
        r.estimatedFee = priced.estimatedFee();
        return r;
    }

    /**
     * Historical details payload (BR-18): snake_case keys, and the date / timestamp values as the java.sql types
     * the legacy code put in the map, so that their JSON rendering is unchanged.
     */
    static Map<String, Object> toDetails(PricedOutboundPayment priced) {
        OutboundPayment payment = priced.payment();
        Map<String, Object> response = new HashMap<>();
        response.put("id", payment.id().value().toString());
        response.put("instruction_id", payment.instructionId());
        response.put("amount", payment.amount().amount());
        response.put("currency", payment.amount().currency());
        response.put("debtor_iban", payment.debtorIban());
        response.put("debtor_name", payment.debtorName());
        response.put("destination_bic", payment.destinationBic());
        response.put("requested_execution_date",
                payment.requestedExecutionDate() == null ? null : Date.valueOf(payment.requestedExecutionDate()));
        response.put("charges_type", payment.chargesType().code());
        response.put("status", payment.status());
        response.put("created_at", payment.audit() == null ? null : toTimestamp(payment.audit().createdAt()));
        response.put("updated_at", payment.audit() == null ? null : toTimestamp(payment.audit().updatedAt()));
        response.put("estimatedFee", priced.estimatedFee());
        return response;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
