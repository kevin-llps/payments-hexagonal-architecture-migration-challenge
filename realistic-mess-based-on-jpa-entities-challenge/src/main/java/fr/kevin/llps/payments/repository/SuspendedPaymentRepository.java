package fr.kevin.llps.payments.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.SuspendedPaymentSearchRequest;
import fr.kevin.llps.payments.entity.SuspendedPayment;
import fr.kevin.llps.payments.util.PaymentUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class SuspendedPaymentRepository {

    @PersistenceContext
    private EntityManager em;

    public List<SuspendedPayment> search(SuspendedPaymentSearchRequest req) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p FROM SuspendedPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (req.debtorIban != null && !req.debtorIban.isBlank()) {
            jpql.append(" AND p.debtorIban = :debtorIban");
            params.put("debtorIban", req.debtorIban.trim());
        }
        if (req.holdReason != null && !req.holdReason.isBlank()) {
            jpql.append(" AND p.holdReason = :holdReason");
            params.put("holdReason", req.holdReason.trim().toUpperCase());
        }
        if (req.reviewStatus != null && !req.reviewStatus.isBlank()) {
            jpql.append(" AND p.reviewStatus = :reviewStatus");
            params.put("reviewStatus", req.reviewStatus.trim().toUpperCase());
        }
        if (req.heldFrom != null && !req.heldFrom.isBlank()) {
            jpql.append(" AND p.heldAt >= :heldFrom");
            params.put("heldFrom", PaymentUtils.parseTimestamp(req.heldFrom));
        }
        if (req.heldTo != null && !req.heldTo.isBlank()) {
            jpql.append(" AND p.heldAt <= :heldTo");
            params.put("heldTo", PaymentUtils.parseTimestamp(req.heldTo));
        }
        jpql.append(" ORDER BY p.heldAt DESC");

        TypedQuery<SuspendedPayment> query = em.createQuery(jpql.toString(), SuspendedPayment.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        query.setMaxResults(PaymentUtils.limitOrDefault(req.limit));
        return query.getResultList();
    }

    public SuspendedPayment findById(String id) {
        return em.find(SuspendedPayment.class, UUID.fromString(id.trim()));
    }
}
