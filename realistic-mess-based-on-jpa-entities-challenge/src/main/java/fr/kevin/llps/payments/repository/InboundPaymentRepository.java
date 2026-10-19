package fr.kevin.llps.payments.repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InboundPaymentSearchRequest;
import fr.kevin.llps.payments.entity.InboundPayment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class InboundPaymentRepository {

    @PersistenceContext
    private EntityManager em;

    public List<InboundPaymentResponse> search(InboundPaymentSearchRequest req) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p FROM InboundPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (req.creditorIban != null && !req.creditorIban.isBlank()) {
            jpql.append(" AND p.creditorIban = :creditorIban");
            params.put("creditorIban", req.creditorIban.trim());
        }
        if (req.originBic != null && !req.originBic.isBlank()) {
            jpql.append(" AND UPPER(p.originBic) LIKE :originBic");
            params.put("originBic", "%" + req.originBic.trim().toUpperCase() + "%");
        }
        if (req.status != null && !req.status.isBlank()) {
            jpql.append(" AND p.status = :status");
            params.put("status", req.status.trim().toUpperCase());
        }
        if (req.currency != null && !req.currency.isBlank()) {
            jpql.append(" AND p.currency = :currency");
            params.put("currency", req.currency.trim().toUpperCase());
        }
        if (req.valueDateFrom != null && !req.valueDateFrom.isBlank()) {
            jpql.append(" AND p.valueDate >= :valueDateFrom");
            params.put("valueDateFrom", Date.valueOf(req.valueDateFrom.trim()));
        }
        if (req.valueDateTo != null && !req.valueDateTo.isBlank()) {
            jpql.append(" AND p.valueDate <= :valueDateTo");
            params.put("valueDateTo", Date.valueOf(req.valueDateTo.trim()));
        }
        jpql.append(" ORDER BY p.valueDate DESC, p.createdAt DESC");

        TypedQuery<InboundPayment> query = em.createQuery(jpql.toString(), InboundPayment.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        query.setMaxResults(req.limit == null ? 50 : req.limit);
        List<InboundPaymentResponse> result = new ArrayList<>();
        for (InboundPayment payment : query.getResultList()) {
            result.add(toResponse(payment));
        }
        return result;
    }

    public InboundPaymentResponse findById(String id) {
        InboundPayment payment = em.find(InboundPayment.class, UUID.fromString(id.trim()));
        return payment == null ? null : toResponse(payment);
    }

    private InboundPaymentResponse toResponse(InboundPayment payment) {
        InboundPaymentResponse r = new InboundPaymentResponse();
        r.id = payment.id.toString();
        r.creditTransferRef = payment.creditTransferRef;
        r.amount = payment.amount;
        r.currency = payment.currency;
        r.creditorIban = payment.creditorIban;
        r.creditorName = payment.creditorName;
        r.originBic = payment.originBic;
        r.valueDate = payment.valueDate == null ? null : payment.valueDate.toString();
        r.settlementDate = payment.settlementDate == null ? null : payment.settlementDate.toString();
        r.status = payment.status;
        return r;
    }
}
