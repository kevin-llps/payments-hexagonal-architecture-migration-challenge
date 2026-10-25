package fr.kevin.llps.payments.repository;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentSearchRequest;
import fr.kevin.llps.payments.entity.InstantPayment;
import fr.kevin.llps.payments.util.PaymentUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class InstantPaymentRepository {

    @PersistenceContext
    private EntityManager em;

    public List<InstantPaymentResponse> search(InstantPaymentSearchRequest req) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT p FROM InstantPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (req.endToEndId != null && !req.endToEndId.isBlank()) {
            jpql.append(" AND p.endToEndId = :endToEndId");
            params.put("endToEndId", req.endToEndId.trim());
        }
        if (req.debtorIban != null && !req.debtorIban.isBlank()) {
            jpql.append(" AND UPPER(p.debtorIban) LIKE :debtorIban");
            params.put("debtorIban", PaymentUtils.likePattern(req.debtorIban));
        }
        if (req.creditorIban != null && !req.creditorIban.isBlank()) {
            jpql.append(" AND UPPER(p.creditorIban) LIKE :creditorIban");
            params.put("creditorIban", PaymentUtils.likePattern(req.creditorIban));
        }
        if (req.status != null && !req.status.isBlank()) {
            jpql.append(" AND p.status = :status");
            params.put("status", req.status.trim().toUpperCase());
        }
        if (req.currency != null && !req.currency.isBlank()) {
            jpql.append(" AND p.currency = :currency");
            params.put("currency", req.currency.trim().toUpperCase());
        }
        if (req.executionFrom != null && !req.executionFrom.isBlank()) {
            Timestamp from = parseTimestampValue(req.executionFrom.trim());
            if (from != null) {
                jpql.append(" AND p.executionDatetime >= :executionFrom");
                params.put("executionFrom", from);
            }
        }
        if (req.executionTo != null && !req.executionTo.isBlank()) {
            Timestamp to = parseEndOfDay(req.executionTo.trim());
            if (to != null) {
                jpql.append(" AND p.executionDatetime <= :executionTo");
                params.put("executionTo", to);
            }
        }
        jpql.append(" ORDER BY p.executionDatetime DESC");

        TypedQuery<InstantPayment> query = em.createQuery(jpql.toString(), InstantPayment.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        query.setMaxResults(PaymentUtils.limitOrDefault(req.limit));
        List<InstantPaymentResponse> result = new java.util.ArrayList<>();
        for (InstantPayment payment : query.getResultList()) {
            result.add(toResponse(payment));
        }
        return result;
    }

    public InstantPaymentResponse findById(String id) {
        InstantPayment payment = em.find(InstantPayment.class, UUID.fromString(id.trim()));
        return payment == null ? null : toResponse(payment);
    }

    private InstantPaymentResponse toResponse(InstantPayment payment) {
        InstantPaymentResponse r = new InstantPaymentResponse();
        r.id = payment.id.toString();
        r.endToEndId = payment.endToEndId;
        r.uetr = payment.uetr;
        r.amount = payment.amount;
        r.currency = payment.currency;
        r.debtorIban = payment.debtorIban;
        r.debtorName = payment.debtorName;
        r.creditorIban = payment.creditorIban;
        r.creditorName = payment.creditorName;
        r.executionDatetime = payment.executionDatetime == null ? null : PaymentUtils.FMT.format(payment.executionDatetime);
        r.status = payment.status;
        r.scheme = payment.scheme;
        return r;
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

    private static Timestamp parseEndOfDay(String raw) {
        String s = raw.replace('T', ' ');
        if (s.length() == 10) {
            s = s + " 23:59:59";
        }
        try {
            return Timestamp.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
