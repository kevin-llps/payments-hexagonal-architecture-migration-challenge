package fr.kevin.llps.payments.compliancehold;

import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentCommand;
import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentUseCase;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentRecordRepository;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class RecordSuspendedPaymentService implements RecordSuspendedPaymentUseCase {

    private final SuspendedPaymentRecordRepository repository;
    private final Clock clock;

    public RecordSuspendedPaymentService(SuspendedPaymentRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void record(RecordSuspendedPaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        repository.upsert(new SuspendedPayment(
                PaymentId.parse(command.id()),
                Money.of(command.amount(), command.currency()),
                command.debtorIban(),
                command.debtorName(),
                command.creditorIban(),
                command.creditorName(),
                command.holdReason(),
                command.complianceCaseRef(),
                command.reviewStatus(),
                command.heldAt(),
                command.releasedAt(),
                AuditTimestamps.recordedAt(now)));
    }
}
