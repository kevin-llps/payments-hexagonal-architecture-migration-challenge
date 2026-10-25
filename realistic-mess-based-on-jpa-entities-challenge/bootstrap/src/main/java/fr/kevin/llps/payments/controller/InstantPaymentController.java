package fr.kevin.llps.payments.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentSearchRequest;
import fr.kevin.llps.payments.service.InstantPaymentService;

@RestController
@RequestMapping("/api/v1/instant-payments")
public class InstantPaymentController {

    @Autowired
    private InstantPaymentService service;

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
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.endToEndId = endToEndId;
        request.debtorIban = debtorIban;
        request.creditorIban = creditorIban;
        request.status = status;
        request.currency = currency;
        request.executionFrom = executionFrom;
        request.executionTo = executionTo;
        request.limit = limit;
        return service.search(request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstantPaymentResponse> get(@PathVariable String id) {
        InstantPaymentResponse response = service.getById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
