package fr.kevin.llps.payments.processing.inbound.port.in;

public interface RecordInboundPaymentUseCase {

    void record(RecordInboundPaymentCommand command);
}
