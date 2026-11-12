package fr.kevin.llps.payments.adapter.in.rest.recall;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.kevin.llps.payments.domain.recall.port.in.GetRecallDetails;
import fr.kevin.llps.payments.domain.recall.port.in.SearchRecalls;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

@RestController
@RequestMapping("/api/v1/recalls")
public class RestRecallController {

    private final SearchRecalls searchRecalls;
    private final GetRecallDetails getRecallDetails;
    private final RestRecallResponseMapper responseMapper = new RestRecallResponseMapper();

    public RestRecallController(SearchRecalls searchRecalls, GetRecallDetails getRecallDetails) {
        this.searchRecalls = searchRecalls;
        this.getRecallDetails = getRecallDetails;
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
        RecallSearchQuery query = new RecallSearchQuery(recallType, originalPaymentId, originalPaymentType, reasonCode,
                status, initiatedFrom, initiatedTo, limit);
        return searchRecalls.search(query).stream().map(responseMapper::toResponse).toList();
    }

    /** The path id is trimmed before parsing (BR-API-06); unknown ids give 404 with an empty body (BR-API-04). */
    @GetMapping("/{id}")
    public ResponseEntity<RecallResponse> get(@PathVariable String id) {
        return getRecallDetails.getDetails(PaymentId.parse(id.trim()))
                .map(responseMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
