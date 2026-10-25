package fr.kevin.llps.payments.repository;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.RecallSearchRequest;
import fr.kevin.llps.payments.entity.Recall;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class RecallRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Recall> search(RecallSearchRequest req) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p FROM Recall p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (req.recallType != null && !req.recallType.isBlank()) {
            jpql.append(" AND p.recallType = :recallType");
            params.put("recallType", req.recallType.trim().toUpperCase());
        }
        if (req.originalPaymentId != null && !req.originalPaymentId.isBlank()) {
            jpql.append(" AND p.originalPaymentId = :originalPaymentId");
            params.put("originalPaymentId", UUID.fromString(req.originalPaymentId.trim()));
        }
        if (req.originalPaymentType != null && !req.originalPaymentType.isBlank()) {
            jpql.append(" AND p.originalPaymentType = :originalPaymentType");
            params.put("originalPaymentType", req.originalPaymentType.trim().toUpperCase());
        }
        if (req.reasonCode != null && !req.reasonCode.isBlank()) {
            jpql.append(" AND p.reasonCode = :reasonCode");
            params.put("reasonCode", req.reasonCode.trim().toUpperCase());
        }
        if (req.status != null && !req.status.isBlank()) {
            jpql.append(" AND p.status = :status");
            params.put("status", req.status.trim().toUpperCase());
        }
        if (req.initiatedFrom != null && !req.initiatedFrom.isBlank()) {
            Timestamp from = parseTimestampValue(req.initiatedFrom.trim());
            if (from != null) {
                jpql.append(" AND p.initiatedAt >= :initiatedFrom");
                params.put("initiatedFrom", from);
            }
        }
        if (req.initiatedTo != null && !req.initiatedTo.isBlank()) {
            Timestamp to = parseTimestampValue(req.initiatedTo.trim());
            if (to != null) {
                jpql.append(" AND p.initiatedAt <= :initiatedTo");
                params.put("initiatedTo", to);
            }
        }
        // recalls are always capped: business asked for 50 in 2019 and nobody changed it
        jpql.append(" ORDER BY p.initiatedAt DESC");

        TypedQuery<Recall> query = em.createQuery(jpql.toString(), Recall.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        query.setMaxResults(50);
        return query.getResultList();
    }

    public Recall findById(String id) {
        return em.find(Recall.class, UUID.fromString(id.trim()));
    }

    private static Timestamp parseTimestampValue(String raw) {
        String s = raw.replace('T', ' ');
        if (s.length() == 10) {
            s = s + " 00:00:00";
        }
        try {
            return Timestamp.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
