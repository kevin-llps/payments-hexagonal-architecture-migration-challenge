package fr.kevin.llps.payments.domain.inbound.port.out;

import fr.kevin.llps.payments.domain.inbound.InboundPayment;

public interface SaveInboundPayment {

    /** Full-replace upsert keyed by the payment id. */
    void save(InboundPayment payment);
}
