package fr.kevin.llps.payments.domain.recall.port.out;

import java.util.List;
import java.util.Optional;

import fr.kevin.llps.payments.domain.recall.Recall;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public interface LoadRecalls {

    /** Matching recalls ordered by {@code initiatedAt} descending, at most the criteria limit. */
    List<Recall> search(RecallSearchCriteria criteria);

    Optional<Recall> findById(PaymentId id);
}
