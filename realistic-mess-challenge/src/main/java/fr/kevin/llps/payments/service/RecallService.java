package fr.kevin.llps.payments.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kevin.llps.payments.dto.RecallResponse;
import fr.kevin.llps.payments.dto.RecallSearchRequest;
import fr.kevin.llps.payments.model.RecallRow;
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
        List<RecallRow> rows = repository.search(request);
        List<RecallResponse> result = new ArrayList<>();
        for (RecallRow row : rows) {
            result.add(toResponse(row));
        }
        return result;
    }

    public RecallResponse getById(String id) {
        RecallRow row = repository.findById(id);
        return row == null ? null : toResponse(row);
    }

    private RecallResponse toResponse(RecallRow row) {
        RecallResponse r = new RecallResponse();
        r.id = row.id;
        r.recallReference = row.recallReference;
        r.recallType = row.recallType;
        r.originalPaymentId = row.originalPaymentId;
        r.originalPaymentType = row.originalPaymentType;
        r.reasonCode = row.reasonCode;
        r.amount = row.amount;
        r.currency = row.currency;
        r.initiatedAt = row.initiatedAt == null ? null : PaymentUtils.FMT.format(row.initiatedAt);
        r.deadline = row.deadline == null ? null : PaymentUtils.DAY_FMT.format(row.deadline);
        r.status = row.status;
        return r;
    }
}
