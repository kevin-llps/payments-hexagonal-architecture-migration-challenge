package fr.kevin.llps.payments.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InboundPaymentSearchRequest;
import fr.kevin.llps.payments.service.InboundPaymentService;

@RestController
@RequestMapping("/api/v1/inbound-payments")
public class InboundPaymentController {

    @Autowired
    private InboundPaymentService service;

    @GetMapping("/search")
    public List<InboundPaymentResponse> search(
            @RequestParam(required = false) String creditorIban,
            @RequestParam(required = false) String originBic,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String valueDateFrom,
            @RequestParam(required = false) String valueDateTo,
            @RequestParam(required = false) Integer limit) {
        InboundPaymentSearchRequest request = new InboundPaymentSearchRequest();
        request.creditorIban = creditorIban;
        request.originBic = originBic;
        request.status = status;
        request.currency = currency;
        request.valueDateFrom = valueDateFrom;
        request.valueDateTo = valueDateTo;
        request.limit = limit;
        List<InboundPaymentResponse> responses = service.search(request);
        // "ageInDays" is needed in the list too, ops team asked for it in 2023
        for (InboundPaymentResponse r : responses) {
            if (r.valueDate != null) {
                r.ageInDays = ChronoUnit.DAYS.between(LocalDate.parse(r.valueDate), LocalDate.now());
            }
        }
        return responses;
    }

    @GetMapping("/{id}")
    public ResponseEntity<InboundPaymentResponse> get(@PathVariable String id) {
        InboundPaymentResponse response = service.getById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
