package fr.kevin.llps.payments.compliancehold.adapter.out.persistence;

import fr.kevin.llps.payments.compliancehold.SuspendedPayment;
import fr.kevin.llps.payments.compliancehold.SuspendedPaymentSearchCriteria;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentRecordRepository;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentSearchRepository;
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
 * Translates suspended payment criteria into JPQL; every filter decision is already made by
 * {@link SuspendedPaymentSearchCriteria}.
 */
@Repository
public class JpaSuspendedPaymentAdapter implements SuspendedPaymentSearchRepository, SuspendedPaymentRecordRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<SuspendedPayment> search(SuspendedPaymentSearchCriteria criteria) {
        if (criteria.unsatisfiable()) {
            return List.of();
        }
        StringBuilder jpql = new StringBuilder("SELECT p FROM SuspendedPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (criteria.debtorIban() != null) {
            jpql.append(" AND p.debtorIban = :debtorIban");
            params.put("debtorIban", criteria.debtorIban());
        }
        if (criteria.holdReason() != null) {
            jpql.append(" AND p.holdReason = :holdReason");
            params.put("holdReason", criteria.holdReason());
        }
        if (criteria.reviewStatus() != null) {
            jpql.append(" AND p.reviewStatus = :reviewStatus");
            params.put("reviewStatus", criteria.reviewStatus());
        }
        if (criteria.heldFrom() != null) {
            jpql.append(" AND p.heldAt >= :heldFrom");
            params.put("heldFrom", Timestamp.valueOf(criteria.heldFrom()));
        }
        if (criteria.heldTo() != null) {
            jpql.append(" AND p.heldAt <= :heldTo");
            params.put("heldTo", Timestamp.valueOf(criteria.heldTo()));
        }
        jpql.append(" ORDER BY p.heldAt DESC");
        TypedQuery<fr.kevin.llps.payments.compliancehold.adapter.out.persistence.entity.SuspendedPayment> query =
                em.createQuery(jpql.toString(),
                        fr.kevin.llps.payments.compliancehold.adapter.out.persistence.entity.SuspendedPayment.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(SuspendedPaymentJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<SuspendedPayment> findById(PaymentId id) {
        return Optional.ofNullable(em.find(
                fr.kevin.llps.payments.compliancehold.adapter.out.persistence.entity.SuspendedPayment.class,
                id.value())).map(SuspendedPaymentJpaMapper::toDomain);
    }

    @Override
    @Transactional
    public void upsert(SuspendedPayment payment) {
        em.merge(SuspendedPaymentJpaMapper.toEntity(payment));
    }
}
