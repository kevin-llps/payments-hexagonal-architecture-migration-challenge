package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.recall.port.in.RecordRecallCommand;
import fr.kevin.llps.payments.recall.port.in.RecordRecallUseCase;
import fr.kevin.llps.payments.recall.port.out.RecallRecordRepository;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class RecordRecallService implements RecordRecallUseCase {

    private final RecallRecordRepository repository;
    private final Clock clock;

    public RecordRecallService(RecallRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void record(RecordRecallCommand command) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        repository.upsert(Recall.record(
                PaymentId.parse(command.id()),
                command.recallReference(),
                command.recallType(),
                PaymentId.parse(command.originalPaymentId()),
                command.originalPaymentType(),
                command.reasonCode(),
                command.amount(),
                command.currency(),
                command.initiatedAt(),
                command.deadline(),
                command.status(),
                AuditTimestamps.recordedAt(now)));
    }
}
