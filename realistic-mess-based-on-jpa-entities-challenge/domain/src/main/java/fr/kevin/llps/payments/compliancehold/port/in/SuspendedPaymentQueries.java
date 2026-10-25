package fr.kevin.llps.payments.compliancehold.port.in;

import fr.kevin.llps.payments.compliancehold.SuspendedPayment;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface SuspendedPaymentQueries {

    List<SuspendedPayment> search(SuspendedPaymentSearchQuery query);

    Optional<SuspendedPayment> get(PaymentId id);
}
