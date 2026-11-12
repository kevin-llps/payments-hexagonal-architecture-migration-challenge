package fr.kevin.llps.payments.domain.outbound;

import java.time.Clock;
import java.time.LocalDateTime;

import fr.kevin.llps.payments.domain.outbound.command.PersistOutboundPaymentCommand;
import fr.kevin.llps.payments.domain.outbound.port.in.PersistOutboundPayment;
import fr.kevin.llps.payments.domain.outbound.port.out.SaveOutboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;

public final class PersistOutboundPaymentService implements PersistOutboundPayment {

    private final SaveOutboundPayment saveOutboundPayment;
    private final Clock clock;

    public PersistOutboundPaymentService(SaveOutboundPayment saveOutboundPayment, Clock clock) {
        this.saveOutboundPayment = saveOutboundPayment;
        this.clock = clock;
    }

    @Override
    public void persist(PersistOutboundPaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        saveOutboundPayment.save(OutboundPayment.builder(command.id())
                .instructionId(command.instructionId())
                .money(Money.of(command.amount(), command.currency()))
                .debtorIban(command.debtorIban())
                .debtorName(command.debtorName())
                .destinationBic(command.destinationBic())
                .requestedExecutionDate(command.requestedExecutionDate())
                .chargesType(command.chargesType())
                .status(command.status())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
