package fr.kevin.llps.payments.recall.adapter.out.persistence;

import fr.kevin.llps.payments.recall.Recall;
import fr.kevin.llps.payments.recall.RecallSearchCriteria;
import fr.kevin.llps.payments.recall.port.out.RecallRecordRepository;
import fr.kevin.llps.payments.recall.port.out.RecallSearchRepository;
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
 * Translates recall criteria into JPQL; every filter decision is already made by {@link RecallSearchCriteria}.
 */
@Repository
public class JpaRecallAdapter implements RecallSearchRepository, RecallRecordRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Recall> search(RecallSearchCriteria criteria) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM Recall p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (criteria.recallType() != null) {
            jpql.append(" AND p.recallType = :recallType");
            params.put("recallType", criteria.recallType());
        }
        if (criteria.originalPaymentId() != null) {
            jpql.append(" AND p.originalPaymentId = :originalPaymentId");
            params.put("originalPaymentId", criteria.originalPaymentId().value());
        }
        if (criteria.originalPaymentType() != null) {
            jpql.append(" AND p.originalPaymentType = :originalPaymentType");
            params.put("originalPaymentType", criteria.originalPaymentType());
        }
        if (criteria.reasonCode() != null) {
            jpql.append(" AND p.reasonCode = :reasonCode");
            params.put("reasonCode", criteria.reasonCode());
        }
        if (criteria.status() != null) {
            jpql.append(" AND p.status = :status");
            params.put("status", criteria.status());
        }
        if (criteria.initiatedFrom() != null) {
            jpql.append(" AND p.initiatedAt >= :initiatedFrom");
            params.put("initiatedFrom", Timestamp.valueOf(criteria.initiatedFrom()));
        }
        if (criteria.initiatedTo() != null) {
            jpql.append(" AND p.initiatedAt <= :initiatedTo");
            params.put("initiatedTo", Timestamp.valueOf(criteria.initiatedTo()));
        }
        jpql.append(" ORDER BY p.initiatedAt DESC");
        TypedQuery<fr.kevin.llps.payments.recall.adapter.out.persistence.entity.Recall> query =
                em.createQuery(jpql.toString(), fr.kevin.llps.payments.recall.adapter.out.persistence.entity.Recall.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(RecallJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<Recall> findById(PaymentId id) {
        return Optional.ofNullable(em.find(fr.kevin.llps.payments.recall.adapter.out.persistence.entity.Recall.class,
                id.value())).map(RecallJpaMapper::toDomain);
    }

    @Override
    @Transactional
    public void upsert(Recall recall) {
        em.merge(RecallJpaMapper.toEntity(recall));
    }
}
