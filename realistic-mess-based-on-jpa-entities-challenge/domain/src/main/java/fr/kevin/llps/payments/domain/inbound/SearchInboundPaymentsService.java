package fr.kevin.llps.payments.domain.inbound;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import fr.kevin.llps.payments.domain.inbound.port.in.SearchInboundPayments;
import fr.kevin.llps.payments.domain.inbound.port.out.LoadInboundPayments;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchQuery;

public final class SearchInboundPaymentsService implements SearchInboundPayments {

    private final LoadInboundPayments loadInboundPayments;
    private final Clock clock;

    public SearchInboundPaymentsService(LoadInboundPayments loadInboundPayments, Clock clock) {
        this.loadInboundPayments = loadInboundPayments;
        this.clock = clock;
    }

    @Override
    public List<InboundPaymentView> search(InboundPaymentSearchQuery query) {
        InboundPaymentSearchCriteria criteria = InboundPaymentSearchCriteria.from(query.creditorIban(),
                query.originBic(), query.status(), query.currency(), query.valueDateFrom(), query.valueDateTo(),
                query.limit());
        List<InboundPayment> payments = loadInboundPayments.search(criteria);
        // One clock reading per request (approved at the IT-05 scope gate).
        LocalDate today = LocalDate.now(clock);
        return payments.stream()
                .map(payment -> new InboundPaymentView(payment, payment.ageInDaysOn(today).orElse(null)))
                .toList();
    }
}
