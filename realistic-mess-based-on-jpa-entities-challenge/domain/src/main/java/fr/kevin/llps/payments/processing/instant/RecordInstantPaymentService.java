package fr.kevin.llps.payments.processing.instant;

import fr.kevin.llps.payments.processing.instant.port.in.RecordInstantPaymentCommand;
import fr.kevin.llps.payments.processing.instant.port.in.RecordInstantPaymentUseCase;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentRecordRepository;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class RecordInstantPaymentService implements RecordInstantPaymentUseCase {

    private final InstantPaymentRecordRepository repository;
    private final Clock clock;

    public RecordInstantPaymentService(InstantPaymentRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void record(RecordInstantPaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        repository.upsert(InstantPayment.record(
                PaymentId.parse(command.id()),
                command.endToEndId(),
                command.uetr(),
                Money.of(command.amount(), command.currency()),
                command.debtorIban(),
                command.debtorName(),
                command.creditorIban(),
                command.creditorName(),
                command.executionDatetime(),
                command.status(),
                command.scheme(),
                AuditTimestamps.recordedAt(now)));
    }
}
