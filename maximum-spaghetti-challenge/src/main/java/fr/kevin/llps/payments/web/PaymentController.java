package fr.kevin.llps.payments.web;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.kevin.llps.payments.mess.PaymentsGodService;

/**
 * Thin HTTP layer: every endpoint just forwards everything to the god service.
 */
@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    @Autowired
    private PaymentsGodService godService;

    @GetMapping("/instant-payments/search")
    public List<Map<String, Object>> searchInstantPayments(
            @RequestParam(required = false) String endToEndId,
            @RequestParam(required = false) String debtorIban,
            @RequestParam(required = false) String creditorIban,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String executionFrom,
            @RequestParam(required = false) String executionTo,
            @RequestParam(required = false) Integer limit) {
        return godService.searchInstantPayments(endToEndId, debtorIban, creditorIban, status,
                currency, executionFrom, executionTo, limit);
    }

    @GetMapping("/suspended-payments/search")
    public List<Map<String, Object>> searchSuspendedPayments(
            @RequestParam(required = false) String debtorIban,
            @RequestParam(required = false) String holdReason,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String heldFrom,
            @RequestParam(required = false) String heldTo,
            @RequestParam(required = false) Integer limit) {
        return godService.searchSuspendedPayments(debtorIban, holdReason, reviewStatus, heldFrom,
                heldTo, limit);
    }

    @GetMapping("/inbound-payments/search")
    public List<Map<String, Object>> searchInboundPayments(
            @RequestParam(required = false) String creditorIban,
            @RequestParam(required = false) String originBic,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String valueDateFrom,
            @RequestParam(required = false) String valueDateTo,
            @RequestParam(required = false) Integer limit) {
        return godService.searchInboundPayments(creditorIban, originBic, status, currency,
                valueDateFrom, valueDateTo, limit);
    }

    @GetMapping("/outbound-payments/search")
    public List<Map<String, Object>> searchOutboundPayments(
            @RequestParam(required = false) String debtorIban,
            @RequestParam(required = false) String destinationBic,
            @RequestParam(required = false) String chargesType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String executionFrom,
            @RequestParam(required = false) String executionTo,
            @RequestParam(required = false) Integer limit) {
        return godService.searchOutboundPayments(debtorIban, destinationBic, chargesType, status,
                executionFrom, executionTo, limit);
    }

    @GetMapping("/recalls/search")
    public List<Map<String, Object>> searchRecalls(
            @RequestParam(required = false) String recallType,
            @RequestParam(required = false) String originalPaymentId,
            @RequestParam(required = false) String originalPaymentType,
            @RequestParam(required = false) String reasonCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String initiatedFrom,
            @RequestParam(required = false) String initiatedTo,
            @RequestParam(required = false) Integer limit) {
        List<Map<String, Object>> recalls = godService.searchRecalls(recallType, originalPaymentId,
                originalPaymentType, reasonCode, status, initiatedFrom, initiatedTo, limit);
        // hotfix: reports team needs the "daysLeft" as a positive number for overdue recalls
        for (Map<String, Object> recall : recalls) {
            Object daysLeft = recall.get("daysLeft");
            if (daysLeft instanceof Number number && number.longValue() < 0) {
                recall.put("daysLeft", Math.abs(number.longValue()));
            }
        }
        return recalls;
    }

    @GetMapping("/payments/{type}/{id}")
    public Map<String, Object> getPaymentDetails(@PathVariable String type, @PathVariable String id) {
        Map<String, Object> details = godService.getPaymentDetails(type, id);
        if (details == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "payment not found");
        }
        return details;
    }

    @GetMapping("/payments/export")
    public ResponseEntity<String> export(@RequestParam String type) {
        String csv = godService.exportLegacyCsv(type);
        if ("ERROR".equals(csv)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("export failed");
        }
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .body(csv);
    }

    @GetMapping("/internal/stats")
    public Map<String, Object> stats() {
        return godService.getInternalStats();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception e) {
        if (e instanceof ResponseStatusException rse) {
            return ResponseEntity.status(rse.getStatusCode())
                    .body(Map.of("error", String.valueOf(rse.getReason())));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "internal error: " + e.getClass().getSimpleName()));
    }
}
