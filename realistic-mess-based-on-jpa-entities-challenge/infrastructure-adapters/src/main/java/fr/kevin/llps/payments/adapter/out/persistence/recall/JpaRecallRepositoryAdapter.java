package fr.kevin.llps.payments.adapter.out.persistence.recall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.domain.recall.Recall;
import fr.kevin.llps.payments.domain.recall.port.out.LoadRecalls;
import fr.kevin.llps.payments.domain.recall.port.out.SaveRecall;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Hand-written JPQL over the unchanged {@code Recall} entity: exact codes and original payment id, inclusive
 * initiated bounds, {@code initiatedAt} descending, at most 50 rows (BR-RCL-01..06). Saving merges without flushing
 * (BR-ING-06).
 */
@Repository
public class JpaRecallRepositoryAdapter implements LoadRecalls, SaveRecall {

    private final EntityManager entityManager;
    private final JpaRecallMapper mapper = new JpaRecallMapper();

    public JpaRecallRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Recall> search(RecallSearchCriteria criteria) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM Recall p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        criteria.recallType().ifPresent(value -> {
            jpql.append(" AND p.recallType = :recallType");
            params.put("recallType", value.value());
        });
        criteria.originalPaymentId().ifPresent(value -> {
            jpql.append(" AND p.originalPaymentId = :originalPaymentId");
            params.put("originalPaymentId", value.value());
        });
        criteria.originalPaymentType().ifPresent(value -> {
            jpql.append(" AND p.originalPaymentType = :originalPaymentType");
            params.put("originalPaymentType", value.value());
        });
        criteria.reasonCode().ifPresent(value -> {
            jpql.append(" AND p.reasonCode = :reasonCode");
            params.put("reasonCode", value.value());
        });
        criteria.status().ifPresent(value -> {
            jpql.append(" AND p.status = :status");
            params.put("status", value.value());
        });
        criteria.initiatedFrom().value().ifPresent(value -> {
            jpql.append(" AND p.initiatedAt >= :initiatedFrom");
            params.put("initiatedFrom", mapper.toTimestamp(value));
        });
        criteria.initiatedTo().value().ifPresent(value -> {
            jpql.append(" AND p.initiatedAt <= :initiatedTo");
            params.put("initiatedTo", mapper.toTimestamp(value));
        });
        jpql.append(" ORDER BY p.initiatedAt DESC");

        TypedQuery<fr.kevin.llps.payments.entity.Recall> query =
                entityManager.createQuery(jpql.toString(), fr.kevin.llps.payments.entity.Recall.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Recall> findById(PaymentId id) {
        return Optional.ofNullable(entityManager.find(fr.kevin.llps.payments.entity.Recall.class, id.value()))
                .map(mapper::toDomain);
    }

    @Override
    public void save(Recall recall) {
        entityManager.merge(mapper.toEntity(recall));
    }
}
