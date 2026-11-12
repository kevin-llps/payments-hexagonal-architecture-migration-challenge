package fr.kevin.llps.payments.domain.instant.port.out;

import fr.kevin.llps.payments.domain.instant.InstantPayment;

public interface SaveInstantPayment {

    /** Full-replace upsert keyed by the payment id. */
    void save(InstantPayment payment);
}
