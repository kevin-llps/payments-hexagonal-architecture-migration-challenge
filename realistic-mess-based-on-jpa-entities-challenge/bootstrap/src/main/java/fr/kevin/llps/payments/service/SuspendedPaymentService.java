package fr.kevin.llps.payments.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kevin.llps.payments.dto.SuspendedPaymentResponse;
import fr.kevin.llps.payments.dto.SuspendedPaymentSearchRequest;
import fr.kevin.llps.payments.entity.SuspendedPayment;
import fr.kevin.llps.payments.repository.SuspendedPaymentRepository;
import fr.kevin.llps.payments.util.PaymentUtils;

@Service
public class SuspendedPaymentService {

    @Autowired
    private SuspendedPaymentRepository repository;

    public List<SuspendedPaymentResponse> search(SuspendedPaymentSearchRequest request) {
        List<SuspendedPayment> payments = repository.search(request);
        List<SuspendedPaymentResponse> result = new ArrayList<>();
        for (SuspendedPayment payment : payments) {
            SuspendedPaymentResponse r = new SuspendedPaymentResponse();
            r.id = payment.id.toString();
            r.amount = payment.amount;
            r.currency = payment.currency;
            r.debtorIban = payment.debtorIban;
            r.debtorName = payment.debtorName;
            r.creditorIban = payment.creditorIban;
            r.creditorName = payment.creditorName;
            r.holdReason = payment.holdReason;
            r.complianceCaseRef = payment.complianceCaseRef;
            r.reviewStatus = payment.reviewStatus;
            r.heldAt = payment.heldAt == null ? null : PaymentUtils.FMT.format(payment.heldAt);
            r.releasedAt = payment.releasedAt == null ? null : PaymentUtils.FMT.format(payment.releasedAt);
            result.add(r);
        }
        return result;
    }

    public SuspendedPaymentResponse getById(String id) {
        SuspendedPayment payment = repository.findById(id);
        if (payment == null) {
            return null;
        }
        SuspendedPaymentResponse r = new SuspendedPaymentResponse();
        r.id = payment.id.toString();
        r.amount = payment.amount;
        r.currency = payment.currency;
        r.debtorIban = payment.debtorIban;
        r.debtorName = payment.debtorName;
        r.creditorIban = payment.creditorIban;
        r.creditorName = payment.creditorName;
        r.holdReason = payment.holdReason;
        r.complianceCaseRef = payment.complianceCaseRef;
        r.reviewStatus = payment.reviewStatus;
        r.heldAt = payment.heldAt == null ? null : PaymentUtils.FMT.format(payment.heldAt);
        r.releasedAt = payment.releasedAt == null ? null : PaymentUtils.FMT.format(payment.releasedAt);
        return r;
    }
}
