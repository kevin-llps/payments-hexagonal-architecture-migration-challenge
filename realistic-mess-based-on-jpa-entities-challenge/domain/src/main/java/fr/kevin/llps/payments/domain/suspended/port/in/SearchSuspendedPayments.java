package fr.kevin.llps.payments.domain.suspended.port.in;

import java.util.List;

import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchQuery;

public interface SearchSuspendedPayments {

    List<SuspendedPayment> search(SuspendedPaymentSearchQuery query);
}
