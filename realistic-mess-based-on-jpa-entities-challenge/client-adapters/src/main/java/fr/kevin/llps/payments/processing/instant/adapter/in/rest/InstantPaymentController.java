package fr.kevin.llps.payments.processing.instant.adapter.in.rest;

import fr.kevin.llps.payments.processing.instant.adapter.in.rest.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentQueries;
import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentSearchQuery;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/instant-payments")
public class InstantPaymentController {

    private final InstantPaymentQueries instantPayments;

    public InstantPaymentController(InstantPaymentQueries instantPayments) {
        this.instantPayments = instantPayments;
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
        return instantPayments.search(query).stream().map(InstantPaymentRestMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstantPaymentResponse> get(@PathVariable String id) {
        return instantPayments.get(PaymentId.parse(id.trim()))
                .map(InstantPaymentRestMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
