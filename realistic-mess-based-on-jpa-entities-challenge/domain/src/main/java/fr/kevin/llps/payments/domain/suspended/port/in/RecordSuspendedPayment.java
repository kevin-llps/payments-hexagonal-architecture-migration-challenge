package fr.kevin.llps.payments.domain.suspended.port.in;

import fr.kevin.llps.payments.domain.suspended.command.RecordSuspendedPaymentCommand;

public interface RecordSuspendedPayment {

    void record(RecordSuspendedPaymentCommand command);
}
