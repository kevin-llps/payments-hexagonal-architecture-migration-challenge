package fr.kevin.llps.payments.domain.recall.port.out;

import fr.kevin.llps.payments.domain.recall.Recall;

public interface SaveRecall {

    /** Full-replace upsert keyed by the recall id. */
    void save(Recall recall);
}
