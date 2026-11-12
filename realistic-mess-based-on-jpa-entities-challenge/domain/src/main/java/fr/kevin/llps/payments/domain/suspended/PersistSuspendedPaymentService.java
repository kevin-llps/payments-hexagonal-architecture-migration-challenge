package fr.kevin.llps.payments.domain.suspended;

import java.time.Clock;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.suspended.command.PersistSuspendedPaymentCommand;
import fr.kevin.llps.payments.domain.suspended.port.in.PersistSuspendedPayment;
import fr.kevin.llps.payments.domain.suspended.port.out.SaveSuspendedPayment;

public final class PersistSuspendedPaymentService implements PersistSuspendedPayment {

    private final SaveSuspendedPayment saveSuspendedPayment;
    private final Clock clock;

    public PersistSuspendedPaymentService(SaveSuspendedPayment saveSuspendedPayment, Clock clock) {
        this.saveSuspendedPayment = saveSuspendedPayment;
        this.clock = clock;
    }

    @Override
    public void persist(PersistSuspendedPaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        saveSuspendedPayment.save(SuspendedPayment.builder(command.id())
                .money(Money.of(command.amount(), command.currency()))
                .debtorIban(command.debtorIban())
                .debtorName(command.debtorName())
                .creditorIban(command.creditorIban())
                .creditorName(command.creditorName())
                .holdReason(command.holdReason())
                .complianceCaseRef(command.complianceCaseRef())
                .reviewStatus(command.reviewStatus())
                .heldAt(command.heldAt())
                .releasedAt(command.releasedAt())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
