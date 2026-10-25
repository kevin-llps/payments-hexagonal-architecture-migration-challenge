package fr.kevin.llps.payments.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.dto.RecallResponse;
import fr.kevin.llps.payments.dto.RecallSearchRequest;
import fr.kevin.llps.payments.service.RecallService;

@RestController
@RequestMapping("/api/v1/recalls")
public class RecallController {

    @Autowired
    private RecallService service;

    @GetMapping("/search")
    public List<RecallResponse> search(
            @RequestParam(required = false) String recallType,
            @RequestParam(required = false) String originalPaymentId,
            @RequestParam(required = false) String originalPaymentType,
            @RequestParam(required = false) String reasonCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String initiatedFrom,
            @RequestParam(required = false) String initiatedTo,
            @RequestParam(required = false) Integer limit) {
        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = recallType;
        request.originalPaymentId = originalPaymentId;
        request.originalPaymentType = originalPaymentType;
        request.reasonCode = reasonCode;
        request.status = status;
        request.initiatedFrom = initiatedFrom;
        request.initiatedTo = initiatedTo;
        request.limit = limit;
        return service.search(request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecallResponse> get(@PathVariable String id) {
        RecallResponse response = service.getById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
