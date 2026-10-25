package fr.kevin.llps.payments.recall.port.out;

import fr.kevin.llps.payments.recall.Recall;
import fr.kevin.llps.payments.recall.RecallSearchCriteria;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface RecallSearchRepository {

    List<Recall> search(RecallSearchCriteria criteria);

    Optional<Recall> findById(PaymentId id);
}
