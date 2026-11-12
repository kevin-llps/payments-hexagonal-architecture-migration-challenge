package fr.kevin.llps.payments.repository;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.util.PaymentUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class OutboundPaymentRepository {

    @PersistenceContext
    private EntityManager em;

    public List<OutboundPayment> search(OutboundPaymentSearchRequest req) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p FROM OutboundPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (req.debtorIban != null && !req.debtorIban.isBlank()) {
            jpql.append(" AND p.debtorIban = :debtorIban");
            params.put("debtorIban", req.debtorIban.trim());
        }
        if (req.destinationBic != null && !req.destinationBic.isBlank()) {
            // sic: BIC filter behaves like a "contains" search, decided during a hotfix in 2021
            jpql.append(" AND p.destinationBic LIKE :destinationBic");
            params.put("destinationBic", "%" + req.destinationBic.trim().toUpperCase() + "%");
        }
        if (req.chargesType != null && !req.chargesType.isBlank()) {
            jpql.append(" AND p.chargesType = :chargesType");
            params.put("chargesType", req.chargesType.trim().toUpperCase());
        }
        if (req.status != null && !req.status.isBlank()) {
            jpql.append(" AND p.status = :status");
            params.put("status", req.status.trim().toUpperCase());
        }
        if (req.executionFrom != null && !req.executionFrom.isBlank()) {
            jpql.append(" AND p.requestedExecutionDate >= :executionFrom");
            params.put("executionFrom", Date.valueOf(req.executionFrom.trim()));
        }
        if (req.executionTo != null && !req.executionTo.isBlank()) {
            jpql.append(" AND p.requestedExecutionDate <= :executionTo");
            params.put("executionTo", Date.valueOf(req.executionTo.trim()));
        }
        jpql.append(" ORDER BY p.requestedExecutionDate DESC");

        TypedQuery<OutboundPayment> query = em.createQuery(jpql.toString(), OutboundPayment.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        query.setMaxResults(PaymentUtils.limitOrDefault(req.limit));
        return query.getResultList();
    }

    public OutboundPayment findById(String id) {
        return em.find(OutboundPayment.class, UUID.fromString(id.trim()));
    }
}
