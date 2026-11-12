package fr.kevin.llps.payments.domain.inbound;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import fr.kevin.llps.payments.domain.inbound.port.in.GetInboundPaymentDetails;
import fr.kevin.llps.payments.domain.inbound.port.out.LoadInboundPayments;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public final class GetInboundPaymentDetailsService implements GetInboundPaymentDetails {

    private final LoadInboundPayments loadInboundPayments;
    private final Clock clock;

    public GetInboundPaymentDetailsService(LoadInboundPayments loadInboundPayments, Clock clock) {
        this.loadInboundPayments = loadInboundPayments;
        this.clock = clock;
    }

    @Override
    public Optional<InboundPaymentView> getDetails(PaymentId id) {
        return loadInboundPayments.findById(id)
                .map(payment -> new InboundPaymentView(payment, payment.ageInDaysOn(LocalDate.now(clock)).orElse(null)));
    }
}
