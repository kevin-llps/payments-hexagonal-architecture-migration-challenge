package fr.kevin.llps.payments.processing.outbound.adapter.in.rest;

import fr.kevin.llps.payments.processing.outbound.adapter.in.rest.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentQueries;
import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentSearchQuery;
import fr.kevin.llps.payments.shared.domain.MalformedValueException;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/outbound-payments")
public class OutboundPaymentController {

    private final OutboundPaymentQueries outboundPayments;

    public OutboundPaymentController(OutboundPaymentQueries outboundPayments) {
        this.outboundPayments = outboundPayments;
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
        return outboundPayments.search(query).stream().map(OutboundPaymentRestMapper::toResponse).toList();
    }

    /**
     * Details keep their historical contract: a snake_case map priced with the details tariff, and 404 (not 500)
     * for an id that is not a UUID (not trimmed).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        PaymentId paymentId;
        try {
            paymentId = PaymentId.parse(id);
        } catch (MalformedValueException e) {
            return ResponseEntity.notFound().build();
        }
        return outboundPayments.get(paymentId)
                .map(OutboundPaymentRestMapper::toDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
