package fr.kevin.llps.payments.processing.outbound.port.in;

public interface RecordOutboundPaymentUseCase {

    void record(RecordOutboundPaymentCommand command);
}
