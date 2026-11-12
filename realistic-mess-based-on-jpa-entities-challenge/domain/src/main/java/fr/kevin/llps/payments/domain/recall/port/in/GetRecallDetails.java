package fr.kevin.llps.payments.domain.recall.port.in;

import java.util.Optional;

import fr.kevin.llps.payments.domain.recall.Recall;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public interface GetRecallDetails {

    Optional<Recall> getDetails(PaymentId id);
}
