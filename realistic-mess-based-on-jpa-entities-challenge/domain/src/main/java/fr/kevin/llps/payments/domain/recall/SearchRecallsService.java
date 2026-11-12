package fr.kevin.llps.payments.domain.recall;

import java.util.List;

import fr.kevin.llps.payments.domain.recall.port.in.SearchRecalls;
import fr.kevin.llps.payments.domain.recall.port.out.LoadRecalls;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchCriteria;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchQuery;

public final class SearchRecallsService implements SearchRecalls {

    private final LoadRecalls loadRecalls;

    public SearchRecallsService(LoadRecalls loadRecalls) {
        this.loadRecalls = loadRecalls;
    }

    @Override
    public List<Recall> search(RecallSearchQuery query) {
        RecallSearchCriteria criteria = RecallSearchCriteria.from(query.recallType(), query.originalPaymentId(),
                query.originalPaymentType(), query.reasonCode(), query.status(), query.initiatedFrom(),
                query.initiatedTo());
        return loadRecalls.search(criteria);
    }
}
