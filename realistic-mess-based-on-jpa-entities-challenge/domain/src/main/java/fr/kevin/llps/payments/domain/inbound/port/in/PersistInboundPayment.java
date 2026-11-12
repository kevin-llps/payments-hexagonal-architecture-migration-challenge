package fr.kevin.llps.payments.domain.inbound.port.in;

import fr.kevin.llps.payments.domain.inbound.command.PersistInboundPaymentCommand;

public interface PersistInboundPayment {

    void persist(PersistInboundPaymentCommand command);
}
