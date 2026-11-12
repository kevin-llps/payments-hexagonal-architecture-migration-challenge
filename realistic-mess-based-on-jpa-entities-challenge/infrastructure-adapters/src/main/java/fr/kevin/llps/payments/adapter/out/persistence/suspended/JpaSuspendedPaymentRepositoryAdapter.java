package fr.kevin.llps.payments.adapter.out.persistence.suspended;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;
import fr.kevin.llps.payments.domain.suspended.port.out.LoadSuspendedPayments;
import fr.kevin.llps.payments.domain.suspended.port.out.SaveSuspendedPayment;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchCriteria;

/**
 * Hand-written JPQL over the unchanged {@code SuspendedPayment} entity: exact debtor IBAN, exact codes, inclusive
 * held bounds, {@code heldAt} descending (BR-SUS-01..05). Saving merges without flushing, so constraint violations
 * surface at the listener's commit (BR-ING-06).
 */
@Repository
public class JpaSuspendedPaymentRepositoryAdapter implements LoadSuspendedPayments, SaveSuspendedPayment {

    private final EntityManager entityManager;
    private final JpaSuspendedPaymentMapper mapper = new JpaSuspendedPaymentMapper();

    public JpaSuspendedPaymentRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<SuspendedPayment> search(SuspendedPaymentSearchCriteria criteria) {
        if (criteria.matchesNothing()) {
            return List.of();
        }
        StringBuilder jpql = new StringBuilder("SELECT p FROM SuspendedPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        criteria.debtorIban().ifPresent(value -> {
            jpql.append(" AND p.debtorIban = :debtorIban");
            params.put("debtorIban", value);
        });
        criteria.holdReason().ifPresent(value -> {
            jpql.append(" AND p.holdReason = :holdReason");
            params.put("holdReason", value.value());
        });
        criteria.reviewStatus().ifPresent(value -> {
            jpql.append(" AND p.reviewStatus = :reviewStatus");
            params.put("reviewStatus", value.value());
        });
        criteria.heldFrom().value().ifPresent(value -> {
            jpql.append(" AND p.heldAt >= :heldFrom");
            params.put("heldFrom", mapper.toTimestamp(value));
        });
        criteria.heldTo().value().ifPresent(value -> {
            jpql.append(" AND p.heldAt <= :heldTo");
            params.put("heldTo", mapper.toTimestamp(value));
        });
        jpql.append(" ORDER BY p.heldAt DESC");

        TypedQuery<fr.kevin.llps.payments.entity.SuspendedPayment> query =
                entityManager.createQuery(jpql.toString(), fr.kevin.llps.payments.entity.SuspendedPayment.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SuspendedPayment> findById(PaymentId id) {
        return Optional.ofNullable(entityManager.find(fr.kevin.llps.payments.entity.SuspendedPayment.class, id.value()))
                .map(mapper::toDomain);
    }

    @Override
    public void save(SuspendedPayment payment) {
        entityManager.merge(mapper.toEntity(payment));
    }
}
