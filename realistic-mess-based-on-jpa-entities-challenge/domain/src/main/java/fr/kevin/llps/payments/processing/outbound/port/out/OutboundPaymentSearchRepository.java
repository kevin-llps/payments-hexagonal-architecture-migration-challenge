package fr.kevin.llps.payments.processing.outbound.port.out;

import fr.kevin.llps.payments.processing.outbound.OutboundPayment;
import fr.kevin.llps.payments.processing.outbound.OutboundPaymentSearchCriteria;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface OutboundPaymentSearchRepository {

    List<OutboundPayment> search(OutboundPaymentSearchCriteria criteria);

    Optional<OutboundPayment> findById(PaymentId id);
}
