package fr.kevin.llps.payments.processing.inbound.port.out;

import fr.kevin.llps.payments.processing.inbound.InboundPayment;

public interface InboundPaymentRecordRepository {

    /**
     * Inserts the payment, or replaces the stored one with the same id.
     */
    void upsert(InboundPayment payment);
}
