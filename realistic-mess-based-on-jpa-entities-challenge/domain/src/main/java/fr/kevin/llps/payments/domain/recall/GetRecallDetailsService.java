package fr.kevin.llps.payments.domain.recall;

import java.util.Optional;

import fr.kevin.llps.payments.domain.recall.port.in.GetRecallDetails;
import fr.kevin.llps.payments.domain.recall.port.out.LoadRecalls;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public final class GetRecallDetailsService implements GetRecallDetails {

    private final LoadRecalls loadRecalls;

    public GetRecallDetailsService(LoadRecalls loadRecalls) {
        this.loadRecalls = loadRecalls;
    }

    @Override
    public Optional<Recall> getDetails(PaymentId id) {
        return loadRecalls.findById(id);
    }
}
