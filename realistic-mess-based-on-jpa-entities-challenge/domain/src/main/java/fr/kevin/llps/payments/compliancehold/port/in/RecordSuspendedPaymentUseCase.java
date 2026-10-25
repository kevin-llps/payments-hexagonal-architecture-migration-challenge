package fr.kevin.llps.payments.compliancehold.port.in;

public interface RecordSuspendedPaymentUseCase {

    void record(RecordSuspendedPaymentCommand command);
}
