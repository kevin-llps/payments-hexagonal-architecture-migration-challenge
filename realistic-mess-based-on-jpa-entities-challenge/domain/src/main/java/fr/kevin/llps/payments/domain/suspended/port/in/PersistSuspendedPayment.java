package fr.kevin.llps.payments.domain.suspended.port.in;

import fr.kevin.llps.payments.domain.suspended.command.PersistSuspendedPaymentCommand;

public interface PersistSuspendedPayment {

    void persist(PersistSuspendedPaymentCommand command);
}
