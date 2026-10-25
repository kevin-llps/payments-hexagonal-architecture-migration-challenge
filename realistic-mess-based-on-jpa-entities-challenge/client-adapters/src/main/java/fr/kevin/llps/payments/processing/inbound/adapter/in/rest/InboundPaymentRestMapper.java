package fr.kevin.llps.payments.processing.inbound.adapter.in.rest;

import fr.kevin.llps.payments.processing.inbound.InboundPayment;
import fr.kevin.llps.payments.processing.inbound.adapter.in.rest.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentView;

import static fr.kevin.llps.payments.shared.adapter.in.rest.RestDateFormats.date;

final class InboundPaymentRestMapper {

    private InboundPaymentRestMapper() {
    }

    static InboundPaymentResponse toResponse(InboundPaymentView view) {
        InboundPayment payment = view.payment();
        InboundPaymentResponse r = new InboundPaymentResponse();
        r.id = payment.id().value().toString();
        r.creditTransferRef = payment.creditTransferRef();
        r.amount = payment.amount().amount();
        r.currency = payment.amount().currency();
        r.creditorIban = payment.creditorIban();
        r.creditorName = payment.creditorName();
        r.originBic = payment.originBic();
        r.valueDate = date(payment.valueDate());
        r.settlementDate = date(payment.settlementDate());
        r.status = payment.status();
        r.ageInDays = view.ageInDays();
        return r;
    }
}
