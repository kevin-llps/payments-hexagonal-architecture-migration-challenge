package fr.kevin.llps.payments.compliancehold.adapter.in.rest;

import fr.kevin.llps.payments.compliancehold.adapter.in.rest.dto.SuspendedPaymentResponse;
import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentQueries;
import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentSearchQuery;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suspended-payments")
public class SuspendedPaymentController {

    private final SuspendedPaymentQueries suspendedPayments;

    public SuspendedPaymentController(SuspendedPaymentQueries suspendedPayments) {
        this.suspendedPayments = suspendedPayments;
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
        return suspendedPayments.search(query).stream().map(SuspendedPaymentRestMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuspendedPaymentResponse> get(@PathVariable String id) {
        return suspendedPayments.get(PaymentId.parse(id.trim()))
                .map(SuspendedPaymentRestMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
