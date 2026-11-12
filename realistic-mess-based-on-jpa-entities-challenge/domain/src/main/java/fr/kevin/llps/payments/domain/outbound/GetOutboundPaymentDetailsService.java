package fr.kevin.llps.payments.domain.outbound;

import java.util.Optional;

import fr.kevin.llps.payments.domain.outbound.port.in.GetOutboundPaymentDetails;
import fr.kevin.llps.payments.domain.outbound.port.out.LoadOutboundPayments;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public final class GetOutboundPaymentDetailsService implements GetOutboundPaymentDetails {

    private final LoadOutboundPayments loadOutboundPayments;
    private final DetailsFeePolicy detailsFeePolicy;

    public GetOutboundPaymentDetailsService(LoadOutboundPayments loadOutboundPayments,
            DetailsFeePolicy detailsFeePolicy) {
        this.loadOutboundPayments = loadOutboundPayments;
        this.detailsFeePolicy = detailsFeePolicy;
    }

    @Override
    public Optional<OutboundPaymentWithFee> getDetails(PaymentId id) {
        return loadOutboundPayments.findById(id)
                .map(payment -> new OutboundPaymentWithFee(payment, detailsFeePolicy.estimate(payment)));
    }
}
