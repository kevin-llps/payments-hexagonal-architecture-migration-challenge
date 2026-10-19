package fr.kevin.llps.payments.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kevin.llps.payments.dto.SuspendedPaymentResponse;
import fr.kevin.llps.payments.dto.SuspendedPaymentSearchRequest;
import fr.kevin.llps.payments.model.SuspendedPaymentRow;
import fr.kevin.llps.payments.repository.SuspendedPaymentRepository;
import fr.kevin.llps.payments.util.PaymentUtils;

@Service
public class SuspendedPaymentService {

    @Autowired
    private SuspendedPaymentRepository repository;

    public List<SuspendedPaymentResponse> search(SuspendedPaymentSearchRequest request) {
        List<SuspendedPaymentRow> rows = repository.search(request);
        List<SuspendedPaymentResponse> result = new ArrayList<>();
        for (SuspendedPaymentRow row : rows) {
            SuspendedPaymentResponse r = new SuspendedPaymentResponse();
            r.id = row.id;
            r.amount = row.amount;
            r.currency = row.currency;
            r.debtorIban = row.debtorIban;
            r.debtorName = row.debtorName;
            r.creditorIban = row.creditorIban;
            r.creditorName = row.creditorName;
            r.holdReason = row.holdReason;
            r.complianceCaseRef = row.complianceCaseRef;
            r.reviewStatus = row.reviewStatus;
            r.heldAt = row.heldAt == null ? null : PaymentUtils.FMT.format(row.heldAt);
            r.releasedAt = row.releasedAt == null ? null : PaymentUtils.FMT.format(row.releasedAt);
            result.add(r);
        }
        return result;
    }

    public SuspendedPaymentResponse getById(String id) {
        SuspendedPaymentRow row = repository.findById(id);
        if (row == null) {
            return null;
        }
        SuspendedPaymentResponse r = new SuspendedPaymentResponse();
        r.id = row.id;
        r.amount = row.amount;
        r.currency = row.currency;
        r.debtorIban = row.debtorIban;
        r.debtorName = row.debtorName;
        r.creditorIban = row.creditorIban;
        r.creditorName = row.creditorName;
        r.holdReason = row.holdReason;
        r.complianceCaseRef = row.complianceCaseRef;
        r.reviewStatus = row.reviewStatus;
        r.heldAt = row.heldAt == null ? null : PaymentUtils.FMT.format(row.heldAt);
        r.releasedAt = row.releasedAt == null ? null : PaymentUtils.FMT.format(row.releasedAt);
        return r;
    }
}
