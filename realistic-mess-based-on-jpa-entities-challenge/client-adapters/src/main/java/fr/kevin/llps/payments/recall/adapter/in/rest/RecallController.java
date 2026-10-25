package fr.kevin.llps.payments.recall.adapter.in.rest;

import fr.kevin.llps.payments.recall.adapter.in.rest.dto.RecallResponse;
import fr.kevin.llps.payments.recall.port.in.RecallQueries;
import fr.kevin.llps.payments.recall.port.in.RecallSearchQuery;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recalls")
public class RecallController {

    private final RecallQueries recalls;

    public RecallController(RecallQueries recalls) {
        this.recalls = recalls;
    }

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
        RecallSearchQuery query = new RecallSearchQuery(recallType, originalPaymentId, originalPaymentType,
                reasonCode, status, initiatedFrom, initiatedTo, limit);
        return recalls.search(query).stream().map(RecallRestMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecallResponse> get(@PathVariable String id) {
        return recalls.get(PaymentId.parse(id.trim()))
                .map(RecallRestMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
