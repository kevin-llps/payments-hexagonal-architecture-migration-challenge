package fr.kevin.llps.payments.processing.inbound.port.out;

import fr.kevin.llps.payments.processing.inbound.InboundPayment;
import fr.kevin.llps.payments.processing.inbound.InboundPaymentSearchCriteria;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface InboundPaymentSearchRepository {

    List<InboundPayment> search(InboundPaymentSearchCriteria criteria);

    Optional<InboundPayment> findById(PaymentId id);
}
