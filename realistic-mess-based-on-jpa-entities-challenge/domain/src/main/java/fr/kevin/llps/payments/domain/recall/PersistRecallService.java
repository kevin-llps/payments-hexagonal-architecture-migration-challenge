package fr.kevin.llps.payments.domain.recall;

import java.time.Clock;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.recall.command.PersistRecallCommand;
import fr.kevin.llps.payments.domain.recall.port.in.PersistRecall;
import fr.kevin.llps.payments.domain.recall.port.out.SaveRecall;
import fr.kevin.llps.payments.domain.sharedkernel.Money;

public final class PersistRecallService implements PersistRecall {

    private final SaveRecall saveRecall;
    private final Clock clock;

    public PersistRecallService(SaveRecall saveRecall, Clock clock) {
        this.saveRecall = saveRecall;
        this.clock = clock;
    }

    @Override
    public void persist(PersistRecallCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        saveRecall.save(Recall.builder(command.id())
                .recallReference(command.recallReference())
                .recallType(command.recallType())
                .originalPayment(OriginalPaymentReference.of(command.originalPaymentId(), command.originalPaymentType()))
                .reasonCode(command.reasonCode())
                .money(Money.of(command.amount(), command.currency()))
                .initiatedAt(command.initiatedAt())
                .deadline(command.deadline())
                .status(command.status())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
