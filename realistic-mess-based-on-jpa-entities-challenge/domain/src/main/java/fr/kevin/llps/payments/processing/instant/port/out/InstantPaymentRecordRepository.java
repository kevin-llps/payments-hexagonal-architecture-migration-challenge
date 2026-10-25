package fr.kevin.llps.payments.processing.instant.port.out;

import fr.kevin.llps.payments.processing.instant.InstantPayment;

public interface InstantPaymentRecordRepository {

    /**
     * Inserts the payment, or replaces the stored one with the same id.
     */
    void upsert(InstantPayment payment);
}
