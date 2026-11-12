package fr.kevin.llps.payments.adapter.out.persistence.inbound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import fr.kevin.llps.payments.domain.inbound.InboundPayment;
import fr.kevin.llps.payments.domain.inbound.port.out.LoadInboundPayments;
import fr.kevin.llps.payments.domain.inbound.port.out.SaveInboundPayment;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Hand-written JPQL over the unchanged {@code InboundPayment} entity: exact creditor IBAN, case-insensitive contains
 * on the origin BIC ({@code UPPER(col) LIKE %fragment%}), exact codes, inclusive value-date bounds, ordered by value
 * date then creation time, both descending (BR-INB-01..06). Saving merges without flushing (BR-ING-06).
 */
@Repository
public class JpaInboundPaymentRepositoryAdapter implements LoadInboundPayments, SaveInboundPayment {

    private final EntityManager entityManager;
    private final JpaInboundPaymentMapper mapper = new JpaInboundPaymentMapper();

    private static final String WILDCARD = "%";

    public JpaInboundPaymentRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<InboundPayment> search(InboundPaymentSearchCriteria criteria) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM InboundPayment p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        criteria.creditorIban().ifPresent(value -> {
            jpql.append(" AND p.creditorIban = :creditorIban");
            params.put("creditorIban", value);
        });
        criteria.originBicFragment().ifPresent(value -> {
            jpql.append(" AND UPPER(p.originBic) LIKE :originBic");
            params.put("originBic", WILDCARD + value + WILDCARD);
        });
        criteria.status().ifPresent(value -> {
            jpql.append(" AND p.status = :status");
            params.put("status", value.value());
        });
        criteria.currency().ifPresent(value -> {
            jpql.append(" AND p.currency = :currency");
            params.put("currency", value.value());
        });
        criteria.valueDateFrom().ifPresent(value -> {
            jpql.append(" AND p.valueDate >= :valueDateFrom");
            params.put("valueDateFrom", mapper.toDate(value.value()));
        });
        criteria.valueDateTo().ifPresent(value -> {
            jpql.append(" AND p.valueDate <= :valueDateTo");
            params.put("valueDateTo", mapper.toDate(value.value()));
        });
        jpql.append(" ORDER BY p.valueDate DESC, p.createdAt DESC");

        TypedQuery<fr.kevin.llps.payments.entity.InboundPayment> query =
                entityManager.createQuery(jpql.toString(), fr.kevin.llps.payments.entity.InboundPayment.class);
        params.forEach(query::setParameter);
        query.setMaxResults(criteria.limit().value());
        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<InboundPayment> findById(PaymentId id) {
        return Optional.ofNullable(entityManager.find(fr.kevin.llps.payments.entity.InboundPayment.class, id.value()))
                .map(mapper::toDomain);
    }

    @Override
    public void save(InboundPayment payment) {
        entityManager.merge(mapper.toEntity(payment));
    }
}
