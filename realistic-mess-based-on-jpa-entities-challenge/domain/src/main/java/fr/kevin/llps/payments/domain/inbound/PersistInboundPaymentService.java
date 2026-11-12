package fr.kevin.llps.payments.domain.inbound;

import java.time.Clock;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.inbound.command.PersistInboundPaymentCommand;
import fr.kevin.llps.payments.domain.inbound.port.in.PersistInboundPayment;
import fr.kevin.llps.payments.domain.inbound.port.out.SaveInboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;

public final class PersistInboundPaymentService implements PersistInboundPayment {

    private final SaveInboundPayment saveInboundPayment;
    private final Clock clock;

    public PersistInboundPaymentService(SaveInboundPayment saveInboundPayment, Clock clock) {
        this.saveInboundPayment = saveInboundPayment;
        this.clock = clock;
    }

    @Override
    public void persist(PersistInboundPaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        saveInboundPayment.save(InboundPayment.builder(command.id())
                .creditTransferRef(command.creditTransferRef())
                .money(Money.of(command.amount(), command.currency()))
                .creditorIban(command.creditorIban())
                .creditorName(command.creditorName())
                .originBic(command.originBic())
                .valueDate(command.valueDate())
                .settlementDate(command.settlementDate())
                .status(command.status())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
