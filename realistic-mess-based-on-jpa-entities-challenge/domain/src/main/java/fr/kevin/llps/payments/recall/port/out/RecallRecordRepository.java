package fr.kevin.llps.payments.recall.port.out;

import fr.kevin.llps.payments.recall.Recall;

public interface RecallRecordRepository {

    /**
     * Inserts the recall, or replaces the stored one with the same id.
     */
    void upsert(Recall recall);
}
