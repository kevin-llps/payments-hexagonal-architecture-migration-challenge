package fr.kevin.llps.payments.compliancehold.adapter.in.rest;

import fr.kevin.llps.payments.compliancehold.SuspendedPayment;
import fr.kevin.llps.payments.compliancehold.adapter.in.rest.dto.SuspendedPaymentResponse;

import static fr.kevin.llps.payments.shared.adapter.in.rest.RestDateFormats.timestamp;

final class SuspendedPaymentRestMapper {

    private SuspendedPaymentRestMapper() {
    }

    static SuspendedPaymentResponse toResponse(SuspendedPayment payment) {
        SuspendedPaymentResponse r = new SuspendedPaymentResponse();
        r.id = payment.id().value().toString();
        r.amount = payment.amount().amount();
        r.currency = payment.amount().currency();
        r.debtorIban = payment.debtorIban();
        r.debtorName = payment.debtorName();
        r.creditorIban = payment.creditorIban();
        r.creditorName = payment.creditorName();
        r.holdReason = payment.holdReason();
        r.complianceCaseRef = payment.complianceCaseRef();
        r.reviewStatus = payment.reviewStatus();
        r.heldAt = timestamp(payment.heldAt());
        r.releasedAt = timestamp(payment.releasedAt());
        return r;
    }
}
