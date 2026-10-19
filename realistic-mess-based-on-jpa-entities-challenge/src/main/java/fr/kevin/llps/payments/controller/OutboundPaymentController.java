package fr.kevin.llps.payments.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.service.OutboundPaymentService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@RestController
@RequestMapping("/api/v1/outbound-payments")
public class OutboundPaymentController {

    @Autowired
    private OutboundPaymentService service;

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/search")
    public List<OutboundPaymentResponse> search(
            @RequestParam(required = false) String debtorIban,
            @RequestParam(required = false) String destinationBic,
            @RequestParam(required = false) String chargesType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String executionFrom,
            @RequestParam(required = false) String executionTo,
            @RequestParam(required = false) Integer limit) {
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.debtorIban = debtorIban;
        request.destinationBic = destinationBic;
        request.chargesType = chargesType;
        request.status = status;
        request.executionFrom = executionFrom;
        request.executionTo = executionTo;
        request.limit = limit;
        return service.search(request);
    }

    /**
     * Details endpoint written from scratch during an incident, it never went through
     * the service layer and returns a different payload than /search.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        try {
            OutboundPayment payment = em.find(OutboundPayment.class, UUID.fromString(id));
            if (payment == null) {
                return ResponseEntity.notFound().build();
            }
            BigDecimal fee;
            if ("OUR".equals(payment.chargesType)) {
                fee = payment.amount.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
            } else if ("SHA".equals(payment.chargesType)) {
                fee = new BigDecimal("0.50");
            } else {
                fee = BigDecimal.ZERO;
            }
            Map<String, Object> response = new HashMap<>();
            response.put("id", payment.id.toString());
            response.put("instruction_id", payment.instructionId);
            response.put("amount", payment.amount);
            response.put("currency", payment.currency);
            response.put("debtor_iban", payment.debtorIban);
            response.put("debtor_name", payment.debtorName);
            response.put("destination_bic", payment.destinationBic);
            response.put("requested_execution_date", payment.requestedExecutionDate);
            response.put("charges_type", payment.chargesType);
            response.put("status", payment.status);
            response.put("created_at", payment.createdAt);
            response.put("updated_at", payment.updatedAt);
            response.put("estimatedFee", fee);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
