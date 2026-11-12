package fr.kevin.llps.payments.domain.outbound.port.out;

import fr.kevin.llps.payments.domain.outbound.OutboundPayment;

public interface SaveOutboundPayment {

    /** Full-replace upsert keyed by the payment id. */
    void save(OutboundPayment payment);
}
