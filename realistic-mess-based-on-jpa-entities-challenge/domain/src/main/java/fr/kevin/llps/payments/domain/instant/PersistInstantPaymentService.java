package fr.kevin.llps.payments.domain.instant;

import java.time.Clock;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.instant.command.PersistInstantPaymentCommand;
import fr.kevin.llps.payments.domain.instant.port.in.PersistInstantPayment;
import fr.kevin.llps.payments.domain.instant.port.out.SaveInstantPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;

public final class PersistInstantPaymentService implements PersistInstantPayment {

    private final SaveInstantPayment saveInstantPayment;
    private final Clock clock;

    public PersistInstantPaymentService(SaveInstantPayment saveInstantPayment, Clock clock) {
        this.saveInstantPayment = saveInstantPayment;
        this.clock = clock;
    }

    @Override
    public void persist(PersistInstantPaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        saveInstantPayment.save(InstantPayment.builder(command.id())
                .endToEndId(command.endToEndId())
                .uetr(command.uetr())
                .money(Money.of(command.amount(), command.currency()))
                .debtorIban(command.debtorIban())
                .debtorName(command.debtorName())
                .creditorIban(command.creditorIban())
                .creditorName(command.creditorName())
                .executionDatetime(command.executionDatetime())
                .status(command.status())
                .scheme(command.scheme())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
