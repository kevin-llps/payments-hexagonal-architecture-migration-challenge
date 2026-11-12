package fr.kevin.llps.payments.adapter.in.rest.instant;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.domain.instant.port.in.GetInstantPaymentDetails;
import fr.kevin.llps.payments.domain.instant.port.in.SearchInstantPayments;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

@RestController
@RequestMapping("/api/v1/instant-payments")
public class RestInstantPaymentController {

    private final SearchInstantPayments searchInstantPayments;
    private final GetInstantPaymentDetails getInstantPaymentDetails;
    private final RestInstantPaymentResponseMapper responseMapper = new RestInstantPaymentResponseMapper();

    public RestInstantPaymentController(SearchInstantPayments searchInstantPayments,
            GetInstantPaymentDetails getInstantPaymentDetails) {
        this.searchInstantPayments = searchInstantPayments;
        this.getInstantPaymentDetails = getInstantPaymentDetails;
    }

    @GetMapping("/search")
    public List<InstantPaymentResponse> search(
            @RequestParam(required = false) String endToEndId,
            @RequestParam(required = false) String debtorIban,
            @RequestParam(required = false) String creditorIban,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String executionFrom,
            @RequestParam(required = false) String executionTo,
            @RequestParam(required = false) Integer limit) {
        InstantPaymentSearchQuery query = new InstantPaymentSearchQuery(endToEndId, debtorIban, creditorIban, status,
                currency, executionFrom, executionTo, limit);
        return searchInstantPayments.search(query).stream().map(responseMapper::toResponse).toList();
    }

    /** The path id is trimmed before parsing (BR-API-06); unknown ids give 404 with an empty body (BR-API-04). */
    @GetMapping("/{id}")
    public ResponseEntity<InstantPaymentResponse> get(@PathVariable String id) {
        return getInstantPaymentDetails.getDetails(PaymentId.parse(id.trim()))
                .map(responseMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
