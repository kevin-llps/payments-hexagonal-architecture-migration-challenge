package fr.kevin.llps.payments.processing.outbound.port.out;

import fr.kevin.llps.payments.processing.outbound.OutboundPayment;

public interface OutboundPaymentRecordRepository {

    /**
     * Inserts the payment, or replaces the stored one with the same id.
     */
    void upsert(OutboundPayment payment);
}
