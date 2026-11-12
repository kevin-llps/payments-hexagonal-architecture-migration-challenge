package fr.kevin.llps.payments.adapter.in.rest.outbound;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.domain.outbound.port.in.GetOutboundPaymentDetails;
import fr.kevin.llps.payments.domain.outbound.port.in.SearchOutboundPayments;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.InvalidPaymentIdException;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

@RestController
@RequestMapping("/api/v1/outbound-payments")
public class RestOutboundPaymentController {

    private final SearchOutboundPayments searchOutboundPayments;
    private final GetOutboundPaymentDetails getOutboundPaymentDetails;
    // Only its zone is used: the zone in which stored audit timestamps are rendered (BR-OUT-09).
    private final Clock clock;
    private final RestOutboundPaymentResponseMapper responseMapper = new RestOutboundPaymentResponseMapper();

    public RestOutboundPaymentController(SearchOutboundPayments searchOutboundPayments,
            GetOutboundPaymentDetails getOutboundPaymentDetails, Clock clock) {
        this.searchOutboundPayments = searchOutboundPayments;
        this.getOutboundPaymentDetails = getOutboundPaymentDetails;
        this.clock = clock;
    }

    @GetMapping("/search")
    public List<OutboundPaymentResponse> search(
            @RequestParam(required = false) String debtorIban,
            @RequestParam(required = false) String destinationBic,
            @RequestParam(required = false) String chargesType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String executionFrom,
            @RequestParam(required = false) String executionTo,
            @RequestParam(required = false) Integer limit) {
        OutboundPaymentSearchQuery query = new OutboundPaymentSearchQuery(debtorIban, destinationBic, chargesType,
                status, executionFrom, executionTo, limit);
        return searchOutboundPayments.search(query).stream().map(responseMapper::toSearchResponse).toList();
    }

    /**
     * The path id is not trimmed and a malformed one gives 404 without querying (BR-API-06, C-02); the details shape
     * and fee differ from the search on purpose (BR-OUT-08, BR-OUT-09).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        PaymentId paymentId;
        try {
            paymentId = PaymentId.parse(id);
        } catch (InvalidPaymentIdException e) {
            return ResponseEntity.notFound().build();
        }
        RestOutboundPaymentDetailsFormatter formatter = new RestOutboundPaymentDetailsFormatter(clock.getZone());
        return getOutboundPaymentDetails.getDetails(paymentId)
                .map(withFee -> responseMapper.toDetailsResponse(withFee, formatter))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
