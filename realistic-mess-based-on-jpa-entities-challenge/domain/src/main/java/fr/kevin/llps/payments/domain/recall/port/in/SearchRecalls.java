package fr.kevin.llps.payments.domain.recall.port.in;

import java.util.List;

import fr.kevin.llps.payments.domain.recall.Recall;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchQuery;

public interface SearchRecalls {

    List<Recall> search(RecallSearchQuery query);
}
