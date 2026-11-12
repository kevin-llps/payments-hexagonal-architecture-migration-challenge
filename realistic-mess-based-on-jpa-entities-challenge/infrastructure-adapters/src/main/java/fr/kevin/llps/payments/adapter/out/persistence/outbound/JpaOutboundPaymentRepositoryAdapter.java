package fr.kevin.llps.payments.adapter.out.persistence.outbound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.domain.outbound.OutboundPayment;
import fr.kevin.llps.payments.domain.outbound.port.out.LoadOutboundPayments;
import fr.kevin.llps.payments.domain.outbound.port.out.SaveOutboundPayment;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Hand-written JPQL over the unchanged {@code OutboundPayment} entity: exact debtor IBAN, destination BIC contains
 * matched against the column as stored ({@code col LIKE %FRAGMENT%}, no {@code UPPER}, BR-OUT-02), exact codes,
 * inclusive execution-date bounds, requested execution date descending (BR-OUT-01..05). Saving merges without
 * flushing (BR-ING-06).
 */
@Repository
public class JpaOutboundPaymentRepositoryAdapter implements LoadOutboundPayments, SaveOutboundPayment {

    private final EntityManager entityManager;
    private final JpaOutboundPaymentMapper mapper = new JpaOutboundPaymentMapper();

    private static final String WILDCARD = "%";

    public JpaOutboundPaymentRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<OutboundPayment> search(OutboundPaymentSearchCriteria criteria) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM OutboundPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        criteria.debtorIban().ifPresent(value -> {
            jpql.append(" AND p.debtorIban = :debtorIban");
            params.put("debtorIban", value);
        });
        criteria.destinationBicFragment().ifPresent(value -> {
            jpql.append(" AND p.destinationBic LIKE :destinationBic");
            params.put("destinationBic", WILDCARD + value + WILDCARD);
        });
        criteria.chargesType().ifPresent(value -> {
            jpql.append(" AND p.chargesType = :chargesType");
            params.put("chargesType", value.value());
        });
        criteria.status().ifPresent(value -> {
            jpql.append(" AND p.status = :status");
            params.put("status", value.value());
        });
        criteria.executionFrom().ifPresent(value -> {
            jpql.append(" AND p.requestedExecutionDate >= :executionFrom");
            params.put("executionFrom", mapper.toDate(value.value()));
        });
        criteria.executionTo().ifPresent(value -> {
            jpql.append(" AND p.requestedExecutionDate <= :executionTo");
            params.put("executionTo", mapper.toDate(value.value()));
        });
        jpql.append(" ORDER BY p.requestedExecutionDate DESC");

        TypedQuery<fr.kevin.llps.payments.entity.OutboundPayment> query =
                entityManager.createQuery(jpql.toString(), fr.kevin.llps.payments.entity.OutboundPayment.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<OutboundPayment> findById(PaymentId id) {
        return Optional.ofNullable(entityManager.find(fr.kevin.llps.payments.entity.OutboundPayment.class, id.value()))
                .map(mapper::toDomain);
    }

    @Override
    public void save(OutboundPayment payment) {
        entityManager.merge(mapper.toEntity(payment));
    }
}
