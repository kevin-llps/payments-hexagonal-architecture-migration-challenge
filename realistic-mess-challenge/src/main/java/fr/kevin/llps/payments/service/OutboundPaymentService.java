package fr.kevin.llps.payments.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.model.OutboundPaymentRow;
import fr.kevin.llps.payments.repository.OutboundPaymentRepository;
import fr.kevin.llps.payments.util.PaymentUtils;

@Service
public class OutboundPaymentService {

    @Autowired
    private OutboundPaymentRepository repository;

    public List<OutboundPaymentResponse> search(OutboundPaymentSearchRequest request) {
        List<OutboundPaymentRow> rows = repository.search(request);
        List<OutboundPaymentResponse> result = new ArrayList<>();
        for (OutboundPaymentRow row : rows) {
            result.add(toResponse(row));
        }
        return result;
    }

    public OutboundPaymentResponse getById(String id) {
        OutboundPaymentRow row = repository.findById(id);
        return row == null ? null : toResponse(row);
    }

    private OutboundPaymentResponse toResponse(OutboundPaymentRow row) {
        OutboundPaymentResponse r = new OutboundPaymentResponse();
        r.id = row.id;
        r.instructionId = row.instructionId;
        r.amount = row.amount;
        r.currency = row.currency;
        r.debtorIban = row.debtorIban;
        r.debtorName = row.debtorName;
        r.destinationBic = row.destinationBic;
        r.requestedExecutionDate = row.requestedExecutionDate == null ? null : row.requestedExecutionDate.toString();
        r.chargesType = row.chargesType;
        r.status = row.status;
        r.estimatedFee = estimateFee(row);
        return r;
    }

    private BigDecimal estimateFee(OutboundPaymentRow row) {
        if (row.amount == null) {
            return BigDecimal.ZERO;
        }
        if ("OUR".equals(row.chargesType)) {
            return row.amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        }
        if ("BEN".equals(row.chargesType)) {
            return BigDecimal.ZERO;
        }
        return PaymentUtils.DEFAULT_FEE;
    }
}
