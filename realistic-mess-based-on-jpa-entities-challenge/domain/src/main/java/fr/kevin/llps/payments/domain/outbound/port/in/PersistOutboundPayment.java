package fr.kevin.llps.payments.domain.outbound.port.in;

import fr.kevin.llps.payments.domain.outbound.command.PersistOutboundPaymentCommand;

public interface PersistOutboundPayment {

    void persist(PersistOutboundPaymentCommand command);
}
