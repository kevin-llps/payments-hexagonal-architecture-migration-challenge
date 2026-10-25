package fr.kevin.llps.payments.processing.instant.adapter.out.persistence;

import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.processing.instant.InstantPaymentSearchCriteria;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentRecordRepository;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Translates instant payment criteria into JPQL; every filter decision is already made by
 * {@link InstantPaymentSearchCriteria}.
 */
@Repository
public class JpaInstantPaymentAdapter implements InstantPaymentSearchRepository, InstantPaymentRecordRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<InstantPayment> search(InstantPaymentSearchCriteria criteria) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM InstantPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (criteria.endToEndId() != null) {
            jpql.append(" AND p.endToEndId = :endToEndId");
            params.put("endToEndId", criteria.endToEndId());
        }
        if (criteria.debtorIbanContains() != null) {
            jpql.append(" AND UPPER(p.debtorIban) LIKE :debtorIban");
            params.put("debtorIban", "%" + criteria.debtorIbanContains() + "%");
        }
        if (criteria.creditorIbanContains() != null) {
            jpql.append(" AND UPPER(p.creditorIban) LIKE :creditorIban");
            params.put("creditorIban", "%" + criteria.creditorIbanContains() + "%");
        }
        if (criteria.status() != null) {
            jpql.append(" AND p.status = :status");
            params.put("status", criteria.status());
        }
        if (criteria.currency() != null) {
            jpql.append(" AND p.currency = :currency");
            params.put("currency", criteria.currency());
        }
        if (criteria.executionFrom() != null) {
            jpql.append(" AND p.executionDatetime >= :executionFrom");
            params.put("executionFrom", Timestamp.valueOf(criteria.executionFrom()));
        }
        if (criteria.executionTo() != null) {
            jpql.append(" AND p.executionDatetime <= :executionTo");
            params.put("executionTo", Timestamp.valueOf(criteria.executionTo()));
        }
        jpql.append(" ORDER BY p.executionDatetime DESC");
        TypedQuery<fr.kevin.llps.payments.processing.instant.adapter.out.persistence.entity.InstantPayment> query =
                em.createQuery(jpql.toString(), fr.kevin.llps.payments.processing.instant.adapter.out.persistence.entity.InstantPayment.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(InstantPaymentJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<InstantPayment> findById(PaymentId id) {
        return Optional.ofNullable(em.find(fr.kevin.llps.payments.processing.instant.adapter.out.persistence.entity.InstantPayment.class, id.value()))
                .map(InstantPaymentJpaMapper::toDomain);
    }

    @Override
    @Transactional
    public void upsert(InstantPayment payment) {
        em.merge(InstantPaymentJpaMapper.toEntity(payment));
    }
}
