package fr.kevin.llps.payments.adapter.out.persistence.instant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.instant.port.out.LoadInstantPayments;
import fr.kevin.llps.payments.domain.instant.port.out.SaveInstantPayment;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Hand-written JPQL over the unchanged {@code InstantPayment} entity: exact end-to-end id, case-insensitive contains
 * on IBANs ({@code UPPER(col) LIKE %fragment%}, wildcards not escaped), exact codes, inclusive execution bounds,
 * {@code executionDatetime} descending (BR-INS-03..08). Saving merges without flushing (BR-ING-06).
 */
@Repository
public class JpaInstantPaymentRepositoryAdapter implements LoadInstantPayments, SaveInstantPayment {

    private final EntityManager entityManager;
    private final JpaInstantPaymentMapper mapper = new JpaInstantPaymentMapper();

    private static final String WILDCARD = "%";

    public JpaInstantPaymentRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<InstantPayment> search(InstantPaymentSearchCriteria criteria) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM InstantPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        criteria.endToEndId().ifPresent(value -> {
            jpql.append(" AND p.endToEndId = :endToEndId");
            params.put("endToEndId", value);
        });
        criteria.debtorIbanFragment().ifPresent(value -> {
            jpql.append(" AND UPPER(p.debtorIban) LIKE :debtorIban");
            params.put("debtorIban", WILDCARD + value + WILDCARD);
        });
        criteria.creditorIbanFragment().ifPresent(value -> {
            jpql.append(" AND UPPER(p.creditorIban) LIKE :creditorIban");
            params.put("creditorIban", WILDCARD + value + WILDCARD);
        });
        criteria.status().ifPresent(value -> {
            jpql.append(" AND p.status = :status");
            params.put("status", value.value());
        });
        criteria.currency().ifPresent(value -> {
            jpql.append(" AND p.currency = :currency");
            params.put("currency", value.value());
        });
        criteria.executionFrom().value().ifPresent(value -> {
            jpql.append(" AND p.executionDatetime >= :executionFrom");
            params.put("executionFrom", mapper.toTimestamp(value));
        });
        criteria.executionTo().value().ifPresent(value -> {
            jpql.append(" AND p.executionDatetime <= :executionTo");
            params.put("executionTo", mapper.toTimestamp(value));
        });
        jpql.append(" ORDER BY p.executionDatetime DESC");

        TypedQuery<fr.kevin.llps.payments.entity.InstantPayment> query =
                entityManager.createQuery(jpql.toString(), fr.kevin.llps.payments.entity.InstantPayment.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<InstantPayment> findById(PaymentId id) {
        return Optional.ofNullable(entityManager.find(fr.kevin.llps.payments.entity.InstantPayment.class, id.value()))
                .map(mapper::toDomain);
    }

    @Override
    public void save(InstantPayment payment) {
        entityManager.merge(mapper.toEntity(payment));
    }
}
