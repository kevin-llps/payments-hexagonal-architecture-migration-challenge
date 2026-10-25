package fr.kevin.llps.payments.processing.inbound.adapter.out.persistence;

import fr.kevin.llps.payments.processing.inbound.InboundPayment;
import fr.kevin.llps.payments.processing.inbound.InboundPaymentSearchCriteria;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentRecordRepository;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Translates inbound payment criteria into JPQL; every filter decision is already made by
 * {@link InboundPaymentSearchCriteria}.
 */
@Repository
public class JpaInboundPaymentAdapter implements InboundPaymentSearchRepository, InboundPaymentRecordRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<InboundPayment> search(InboundPaymentSearchCriteria criteria) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM InboundPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (criteria.creditorIban() != null) {
            jpql.append(" AND p.creditorIban = :creditorIban");
            params.put("creditorIban", criteria.creditorIban());
        }
        if (criteria.originBicContains() != null) {
            jpql.append(" AND UPPER(p.originBic) LIKE :originBic");
            params.put("originBic", "%" + criteria.originBicContains() + "%");
        }
        if (criteria.status() != null) {
            jpql.append(" AND p.status = :status");
            params.put("status", criteria.status());
        }
        if (criteria.currency() != null) {
            jpql.append(" AND p.currency = :currency");
            params.put("currency", criteria.currency());
        }
        if (criteria.valueDateFrom() != null) {
            jpql.append(" AND p.valueDate >= :valueDateFrom");
            params.put("valueDateFrom", Date.valueOf(criteria.valueDateFrom()));
        }
        if (criteria.valueDateTo() != null) {
            jpql.append(" AND p.valueDate <= :valueDateTo");
            params.put("valueDateTo", Date.valueOf(criteria.valueDateTo()));
        }
        jpql.append(" ORDER BY p.valueDate DESC, p.createdAt DESC");
        TypedQuery<fr.kevin.llps.payments.processing.inbound.adapter.out.persistence.entity.InboundPayment> query =
                em.createQuery(jpql.toString(), fr.kevin.llps.payments.processing.inbound.adapter.out.persistence.entity.InboundPayment.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(InboundPaymentJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<InboundPayment> findById(PaymentId id) {
        return Optional.ofNullable(em.find(fr.kevin.llps.payments.processing.inbound.adapter.out.persistence.entity.InboundPayment.class, id.value()))
                .map(InboundPaymentJpaMapper::toDomain);
    }

    @Override
    @Transactional
    public void upsert(InboundPayment payment) {
        em.merge(InboundPaymentJpaMapper.toEntity(payment));
    }
}
