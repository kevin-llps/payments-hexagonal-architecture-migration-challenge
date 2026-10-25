package fr.kevin.llps.payments.processing.instant.port.in;

public interface RecordInstantPaymentUseCase {

    void record(RecordInstantPaymentCommand command);
}
