package fr.kevin.llps.payments.adapter.in.rest.suspended;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.port.in.GetSuspendedPaymentDetails;
import fr.kevin.llps.payments.domain.suspended.port.in.SearchSuspendedPayments;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchQuery;

@RestController
@RequestMapping("/api/v1/suspended-payments")
public class RestSuspendedPaymentController {

    private final SearchSuspendedPayments searchSuspendedPayments;
    private final GetSuspendedPaymentDetails getSuspendedPaymentDetails;
    private final RestSuspendedPaymentResponseMapper responseMapper = new RestSuspendedPaymentResponseMapper();

    public RestSuspendedPaymentController(SearchSuspendedPayments searchSuspendedPayments,
            GetSuspendedPaymentDetails getSuspendedPaymentDetails) {
        this.searchSuspendedPayments = searchSuspendedPayments;
        this.getSuspendedPaymentDetails = getSuspendedPaymentDetails;
    }

    @GetMapping("/search")
    public List<SuspendedPaymentResponse> search(
            @RequestParam(required = false) String debtorIban,
            @RequestParam(required = false) String holdReason,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String heldFrom,
            @RequestParam(required = false) String heldTo,
            @RequestParam(required = false) Integer limit) {
        SuspendedPaymentSearchQuery query = new SuspendedPaymentSearchQuery(debtorIban, holdReason, reviewStatus,
                heldFrom, heldTo, limit);
        return searchSuspendedPayments.search(query).stream().map(responseMapper::toResponse).toList();
    }

    /** The path id is trimmed before parsing (BR-API-06); unknown ids give 404 with an empty body (BR-API-04). */
    @GetMapping("/{id}")
    public ResponseEntity<SuspendedPaymentResponse> get(@PathVariable String id) {
        return getSuspendedPaymentDetails.getDetails(PaymentId.parse(id.trim()))
                .map(responseMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
