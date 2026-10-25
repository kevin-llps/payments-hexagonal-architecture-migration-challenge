package fr.kevin.llps.payments.recall.port.in;

import fr.kevin.llps.payments.recall.Recall;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface RecallQueries {

    List<Recall> search(RecallSearchQuery query);

    Optional<Recall> get(PaymentId id);
}
