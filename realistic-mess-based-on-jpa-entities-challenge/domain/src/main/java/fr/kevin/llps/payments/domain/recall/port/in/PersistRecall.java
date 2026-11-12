package fr.kevin.llps.payments.domain.recall.port.in;

import fr.kevin.llps.payments.domain.recall.command.PersistRecallCommand;

public interface PersistRecall {

    void persist(PersistRecallCommand command);
}
