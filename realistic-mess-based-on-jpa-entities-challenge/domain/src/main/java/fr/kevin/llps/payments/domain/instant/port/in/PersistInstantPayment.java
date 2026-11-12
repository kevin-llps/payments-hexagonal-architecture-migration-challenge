package fr.kevin.llps.payments.domain.instant.port.in;

import fr.kevin.llps.payments.domain.instant.command.PersistInstantPaymentCommand;

public interface PersistInstantPayment {

    void persist(PersistInstantPaymentCommand command);
}
