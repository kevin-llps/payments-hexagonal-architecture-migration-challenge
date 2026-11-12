package fr.kevin.llps.payments.domain.suspended.port.out;

import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;

public interface SaveSuspendedPayment {

    /** Full-replace upsert keyed by the payment id. */
    void save(SuspendedPayment payment);
}
