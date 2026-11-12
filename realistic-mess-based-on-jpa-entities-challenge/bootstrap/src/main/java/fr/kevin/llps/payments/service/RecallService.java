package fr.kevin.llps.payments.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kevin.llps.payments.dto.RecallResponse;
import fr.kevin.llps.payments.dto.RecallSearchRequest;
import fr.kevin.llps.payments.entity.Recall;
import fr.kevin.llps.payments.repository.RecallRepository;
import fr.kevin.llps.payments.util.PaymentUtils;

@Service
public class RecallService {

    private static final Set<String> VALID_TYPES = Set.of("R-REJ", "R-COR", "R-UPD");

    @Autowired
    private RecallRepository repository;

    public List<RecallResponse> search(RecallSearchRequest request) {
        if (request.recallType != null && !request.recallType.isBlank()
                && !VALID_TYPES.contains(request.recallType.trim().toUpperCase())) {
            throw new IllegalArgumentException("Unknown recall type: " + request.recallType);
        }
        List<Recall> recalls = repository.search(request);
        List<RecallResponse> result = new ArrayList<>();
        for (Recall recall : recalls) {
            result.add(toResponse(recall));
        }
        return result;
    }

    public RecallResponse getById(String id) {
        Recall recall = repository.findById(id);
        return recall == null ? null : toResponse(recall);
    }

    private RecallResponse toResponse(Recall recall) {
        RecallResponse r = new RecallResponse();
        r.id = recall.id.toString();
        r.recallReference = recall.recallReference;
        r.recallType = recall.recallType;
        r.originalPaymentId = recall.originalPaymentId == null ? null : recall.originalPaymentId.toString();
        r.originalPaymentType = recall.originalPaymentType;
        r.reasonCode = recall.reasonCode;
        r.amount = recall.amount;
        r.currency = recall.currency;
        r.initiatedAt = recall.initiatedAt == null ? null : PaymentUtils.FMT.format(recall.initiatedAt);
        r.deadline = recall.deadline == null ? null : PaymentUtils.DAY_FMT.format(recall.deadline);
        r.status = recall.status;
        return r;
    }
}
