package fr.kevin.llps.payments.processing.instant.adapter.in.rest;

import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.processing.instant.adapter.in.rest.dto.InstantPaymentResponse;

import static fr.kevin.llps.payments.shared.adapter.in.rest.RestDateFormats.timestamp;

final class InstantPaymentRestMapper {

    private InstantPaymentRestMapper() {
    }

    static InstantPaymentResponse toResponse(InstantPayment payment) {
        InstantPaymentResponse r = new InstantPaymentResponse();
        r.id = payment.id().value().toString();
        r.endToEndId = payment.endToEndId();
        r.uetr = payment.uetr();
        r.amount = payment.amount().amount();
        r.currency = payment.amount().currency();
        r.debtorIban = payment.debtorIban();
        r.debtorName = payment.debtorName();
        r.creditorIban = payment.creditorIban();
        r.creditorName = payment.creditorName();
        r.executionDatetime = timestamp(payment.executionDatetime());
        r.status = payment.status();
        r.scheme = payment.scheme();
        return r;
    }
}
