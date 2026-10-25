package fr.kevin.llps.payments.processing.outbound;

import fr.kevin.llps.payments.processing.outbound.port.in.RecordOutboundPaymentCommand;
import fr.kevin.llps.payments.processing.outbound.port.in.RecordOutboundPaymentUseCase;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentRecordRepository;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class RecordOutboundPaymentService implements RecordOutboundPaymentUseCase {

    private final OutboundPaymentRecordRepository repository;
    private final Clock clock;

    public RecordOutboundPaymentService(OutboundPaymentRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void record(RecordOutboundPaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        repository.upsert(new OutboundPayment(
                PaymentId.parse(command.id()),
                command.instructionId(),
                Money.of(command.amount(), command.currency()),
                command.debtorIban(),
                command.debtorName(),
                command.destinationBic(),
                command.requestedExecutionDate(),
                new ChargesType(command.chargesType()),
                command.status(),
                AuditTimestamps.recordedAt(now)));
    }
}
