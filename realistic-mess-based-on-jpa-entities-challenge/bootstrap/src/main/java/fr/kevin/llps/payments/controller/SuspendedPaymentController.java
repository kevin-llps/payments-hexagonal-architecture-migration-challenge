package fr.kevin.llps.payments.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.dto.SuspendedPaymentResponse;
import fr.kevin.llps.payments.dto.SuspendedPaymentSearchRequest;
import fr.kevin.llps.payments.service.SuspendedPaymentService;

@RestController
@RequestMapping("/api/v1/suspended-payments")
public class SuspendedPaymentController {

    @Autowired
    private SuspendedPaymentService service;

    @GetMapping("/search")
    public List<SuspendedPaymentResponse> search(
            @RequestParam(required = false) String debtorIban,
            @RequestParam(required = false) String holdReason,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String heldFrom,
            @RequestParam(required = false) String heldTo,
            @RequestParam(required = false) Integer limit) {
        SuspendedPaymentSearchRequest request = new SuspendedPaymentSearchRequest();
        request.debtorIban = debtorIban;
        request.holdReason = holdReason;
        request.reviewStatus = reviewStatus;
        request.heldFrom = heldFrom;
        request.heldTo = heldTo;
        request.limit = limit;
        return service.search(request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuspendedPaymentResponse> get(@PathVariable String id) {
        SuspendedPaymentResponse response = service.getById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
