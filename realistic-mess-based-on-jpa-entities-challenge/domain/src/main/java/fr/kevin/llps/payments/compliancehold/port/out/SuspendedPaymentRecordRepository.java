package fr.kevin.llps.payments.compliancehold.port.out;

import fr.kevin.llps.payments.compliancehold.SuspendedPayment;

public interface SuspendedPaymentRecordRepository {

    /**
     * Inserts the payment, or replaces the stored one with the same id.
     */
    void upsert(SuspendedPayment payment);
}
