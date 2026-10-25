package fr.kevin.llps.payments.processing.inbound.adapter.in.rest;

import fr.kevin.llps.payments.processing.inbound.adapter.in.rest.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentQueries;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentSearchQuery;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inbound-payments")
public class InboundPaymentController {

    private final InboundPaymentQueries inboundPayments;

    public InboundPaymentController(InboundPaymentQueries inboundPayments) {
        this.inboundPayments = inboundPayments;
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
        return inboundPayments.search(query).stream().map(InboundPaymentRestMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InboundPaymentResponse> get(@PathVariable String id) {
        return inboundPayments.get(PaymentId.parse(id.trim()))
                .map(InboundPaymentRestMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
