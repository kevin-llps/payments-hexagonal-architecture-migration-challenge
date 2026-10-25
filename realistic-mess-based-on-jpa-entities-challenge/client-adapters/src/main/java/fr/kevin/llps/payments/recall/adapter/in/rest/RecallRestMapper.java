package fr.kevin.llps.payments.recall.adapter.in.rest;

import fr.kevin.llps.payments.recall.adapter.in.rest.dto.RecallResponse;
import fr.kevin.llps.payments.recall.Recall;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import static fr.kevin.llps.payments.shared.adapter.in.rest.RestDateFormats.date;
import static fr.kevin.llps.payments.shared.adapter.in.rest.RestDateFormats.timestamp;

final class RecallRestMapper {

    private RecallRestMapper() {
    }

    static RecallResponse toResponse(Recall recall) {
        RecallResponse r = new RecallResponse();
        r.id = recall.id().value().toString();
        r.recallReference = recall.recallReference();
        r.recallType = recall.recallType();
        PaymentId originalPaymentId = recall.originalPayment().id();
        r.originalPaymentId = originalPaymentId == null ? null : originalPaymentId.value().toString();
        r.originalPaymentType = recall.originalPayment().type();
        r.reasonCode = recall.reasonCode();
        r.amount = recall.amount();
        r.currency = recall.currency();
        r.initiatedAt = timestamp(recall.initiatedAt());
        r.deadline = date(recall.deadline());
        r.status = recall.status();
        return r;
    }
}
