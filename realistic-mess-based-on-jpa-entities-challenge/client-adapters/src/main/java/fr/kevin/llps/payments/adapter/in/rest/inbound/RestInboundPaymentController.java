package fr.kevin.llps.payments.adapter.in.rest.inbound;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.domain.inbound.port.in.GetInboundPaymentDetails;
import fr.kevin.llps.payments.domain.inbound.port.in.SearchInboundPayments;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

@RestController
@RequestMapping("/api/v1/inbound-payments")
public class RestInboundPaymentController {

    private final SearchInboundPayments searchInboundPayments;
    private final GetInboundPaymentDetails getInboundPaymentDetails;
    private final RestInboundPaymentResponseMapper responseMapper = new RestInboundPaymentResponseMapper();

    public RestInboundPaymentController(SearchInboundPayments searchInboundPayments,
            GetInboundPaymentDetails getInboundPaymentDetails) {
        this.searchInboundPayments = searchInboundPayments;
        this.getInboundPaymentDetails = getInboundPaymentDetails;
    }

    @GetMapping("/search")
    public List<InboundPaymentResponse> search(
            @RequestParam(required = false) String creditorIban,
            @RequestParam(required = false) String originBic,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String valueDateFrom,
            @RequestParam(required = false) String valueDateTo,
            @RequestParam(required = false) Integer limit) {
        InboundPaymentSearchQuery query = new InboundPaymentSearchQuery(creditorIban, originBic, status, currency,
                valueDateFrom, valueDateTo, limit);
        return searchInboundPayments.search(query).stream().map(responseMapper::toResponse).toList();
    }

    /** The path id is trimmed before parsing (BR-API-06); unknown ids give 404 with an empty body (BR-API-04). */
    @GetMapping("/{id}")
    public ResponseEntity<InboundPaymentResponse> get(@PathVariable String id) {
        return getInboundPaymentDetails.getDetails(PaymentId.parse(id.trim()))
                .map(responseMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
