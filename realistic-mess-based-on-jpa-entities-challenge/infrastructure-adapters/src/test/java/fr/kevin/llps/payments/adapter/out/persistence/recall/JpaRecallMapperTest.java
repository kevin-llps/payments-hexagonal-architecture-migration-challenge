package fr.kevin.llps.payments.adapter.out.persistence.recall;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.kevin.llps.payments.domain.recall.OriginalPaymentReference;
import fr.kevin.llps.payments.domain.recall.Recall;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

class JpaRecallMapperTest {

    private final JpaRecallMapper mapper = new JpaRecallMapper();

    @Test
    void should_map_every_attribute_to_the_entity() {
        // GIVEN
        Recall recall = Recall.builder(PaymentId.parse("00000000-0000-0000-0000-000000000501"))
                .recallReference("RCL-1")
                .recallType("R-COR")
                .originalPayment(OriginalPaymentReference.of(PaymentId.parse("00000000-0000-0000-0000-000000000401"), "OUTBOUND"))
                .reasonCode("AC04")
                .money(Money.of(new BigDecimal("300.00"), "EUR"))
                .initiatedAt(LocalDateTime.of(2026, 10, 16, 12, 0))
                .deadline(LocalDate.of(2026, 10, 18))
                .status("INITIATED")
                .build();

        // WHEN
        fr.kevin.llps.payments.entity.Recall actual = mapper.toEntity(recall);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.id).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000501"));
        assertThat(actual.originalPaymentId).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000401"));
        assertThat(actual.originalPaymentType).isEqualTo("OUTBOUND");
        assertThat(actual.amount).isEqualTo(new BigDecimal("300.00"));
        assertThat(actual.initiatedAt).isEqualTo(Timestamp.valueOf("2026-10-16 12:00:00"));
        assertThat(actual.deadline).isEqualTo(Date.valueOf("2026-10-18"));
        assertThat(actual.createdAt).isNull();
    }

    @Test
    void should_map_an_entity_without_amount_currency_or_original_payment() {
        // GIVEN
        fr.kevin.llps.payments.entity.Recall entity = new fr.kevin.llps.payments.entity.Recall();
        entity.id = UUID.fromString("00000000-0000-0000-0000-000000000685");
        entity.deadline = Date.valueOf("2026-10-18");

        // WHEN
        Recall actual = mapper.toDomain(entity);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.money().amount()).isEmpty();
        assertThat(actual.money().currency()).isEmpty();
        assertThat(actual.originalPayment().paymentId()).isEmpty();
        assertThat(actual.deadline()).contains(LocalDate.of(2026, 10, 18));
    }
}
