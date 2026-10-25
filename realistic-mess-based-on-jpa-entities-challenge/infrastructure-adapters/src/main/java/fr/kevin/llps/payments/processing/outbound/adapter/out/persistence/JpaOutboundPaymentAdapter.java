package fr.kevin.llps.payments.processing.outbound.adapter.out.persistence;

import fr.kevin.llps.payments.processing.outbound.OutboundPayment;
import fr.kevin.llps.payments.processing.outbound.OutboundPaymentSearchCriteria;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentRecordRepository;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentSearchRepository;
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
 * Translates outbound payment criteria into JPQL; every filter decision is already made by
 * {@link OutboundPaymentSearchCriteria}.
 */
@Repository
public class JpaOutboundPaymentAdapter implements OutboundPaymentSearchRepository, OutboundPaymentRecordRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<OutboundPayment> search(OutboundPaymentSearchCriteria criteria) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM OutboundPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (criteria.debtorIban() != null) {
            jpql.append(" AND p.debtorIban = :debtorIban");
            params.put("debtorIban", criteria.debtorIban());
        }
        if (criteria.destinationBicContains() != null) {
            // the stored BIC is compared as stored (not upper-cased), BR-09
            jpql.append(" AND p.destinationBic LIKE :destinationBic");
            params.put("destinationBic", "%" + criteria.destinationBicContains() + "%");
        }
        if (criteria.chargesType() != null) {
            jpql.append(" AND p.chargesType = :chargesType");
            params.put("chargesType", criteria.chargesType());
        }
        if (criteria.status() != null) {
            jpql.append(" AND p.status = :status");
            params.put("status", criteria.status());
        }
        if (criteria.executionFrom() != null) {
            jpql.append(" AND p.requestedExecutionDate >= :executionFrom");
            params.put("executionFrom", Date.valueOf(criteria.executionFrom()));
        }
        if (criteria.executionTo() != null) {
            jpql.append(" AND p.requestedExecutionDate <= :executionTo");
            params.put("executionTo", Date.valueOf(criteria.executionTo()));
        }
        jpql.append(" ORDER BY p.requestedExecutionDate DESC");
        TypedQuery<fr.kevin.llps.payments.processing.outbound.adapter.out.persistence.entity.OutboundPayment> query =
                em.createQuery(jpql.toString(), fr.kevin.llps.payments.processing.outbound.adapter.out.persistence.entity.OutboundPayment.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(OutboundPaymentJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<OutboundPayment> findById(PaymentId id) {
        return Optional.ofNullable(em.find(fr.kevin.llps.payments.processing.outbound.adapter.out.persistence.entity.OutboundPayment.class, id.value()))
                .map(OutboundPaymentJpaMapper::toDomain);
    }

    @Override
    @Transactional
    public void upsert(OutboundPayment payment) {
        em.merge(OutboundPaymentJpaMapper.toEntity(payment));
    }
}
