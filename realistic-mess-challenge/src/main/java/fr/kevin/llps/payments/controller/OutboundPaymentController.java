package fr.kevin.llps.payments.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.service.OutboundPaymentService;

@RestController
@RequestMapping("/api/v1/outbound-payments")
public class OutboundPaymentController {

    @Autowired
    private OutboundPaymentService service;

    @Autowired
    private JdbcTemplate jdbc;

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
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT id, instruction_id, amount, currency, debtor_iban, debtor_name, destination_bic, requested_execution_date, charges_type, status, created_at, updated_at FROM outbound_payments WHERE id = ?::uuid",
                    id);
            BigDecimal amount = (BigDecimal) row.get("amount");
            String chargesType = (String) row.get("charges_type");
            BigDecimal fee;
            if ("OUR".equals(chargesType)) {
                fee = amount.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
            } else if ("SHA".equals(chargesType)) {
                fee = new BigDecimal("0.50");
            } else {
                fee = BigDecimal.ZERO;
            }
            Map<String, Object> response = new HashMap<>(row);
            response.put("estimatedFee", fee);
            return ResponseEntity.ok(response);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
