package fr.kevin.llps.payments.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.repository.OutboundPaymentRepository;
import fr.kevin.llps.payments.util.PaymentUtils;

@Service
public class OutboundPaymentService {

    @Autowired
    private OutboundPaymentRepository repository;

    public List<OutboundPaymentResponse> search(OutboundPaymentSearchRequest request) {
        List<OutboundPayment> payments = repository.search(request);
        List<OutboundPaymentResponse> result = new ArrayList<>();
        for (OutboundPayment payment : payments) {
            result.add(toResponse(payment));
        }
        return result;
    }

    public OutboundPaymentResponse getById(String id) {
        OutboundPayment payment = repository.findById(id);
        return payment == null ? null : toResponse(payment);
    }

    private OutboundPaymentResponse toResponse(OutboundPayment payment) {
        OutboundPaymentResponse r = new OutboundPaymentResponse();
        r.id = payment.id.toString();
        r.instructionId = payment.instructionId;
        r.amount = payment.amount;
        r.currency = payment.currency;
        r.debtorIban = payment.debtorIban;
        r.debtorName = payment.debtorName;
        r.destinationBic = payment.destinationBic;
        r.requestedExecutionDate = payment.requestedExecutionDate == null ? null : payment.requestedExecutionDate.toString();
        r.chargesType = payment.chargesType;
        r.status = payment.status;
        r.estimatedFee = estimateFee(payment);
        return r;
    }

    private BigDecimal estimateFee(OutboundPayment payment) {
        if (payment.amount == null) {
            return BigDecimal.ZERO;
        }
        if ("OUR".equals(payment.chargesType)) {
            return payment.amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        }
        if ("BEN".equals(payment.chargesType)) {
            return BigDecimal.ZERO;
        }
        return PaymentUtils.DEFAULT_FEE;
    }
}
