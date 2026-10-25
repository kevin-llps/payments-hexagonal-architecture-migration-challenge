package fr.kevin.llps.payments.processing.inbound;

import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentCommand;
import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentUseCase;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentRecordRepository;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class RecordInboundPaymentService implements RecordInboundPaymentUseCase {

    private final InboundPaymentRecordRepository repository;
    private final Clock clock;

    public RecordInboundPaymentService(InboundPaymentRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void record(RecordInboundPaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        repository.upsert(new InboundPayment(
                PaymentId.parse(command.id()),
                command.creditTransferRef(),
                Money.of(command.amount(), command.currency()),
                command.creditorIban(),
                command.creditorName(),
                command.originBic(),
                command.valueDate(),
                command.settlementDate(),
                command.status(),
                AuditTimestamps.recordedAt(now)));
    }
}
