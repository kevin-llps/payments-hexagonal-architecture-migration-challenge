package fr.kevin.llps.payments.adapter.out.persistence.instant;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

class JpaInstantPaymentMapperTest {

    private final JpaInstantPaymentMapper mapper = new JpaInstantPaymentMapper();

    @Test
    void should_map_every_attribute_to_the_entity() {
        // GIVEN
        InstantPayment payment = InstantPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000601"))
                .endToEndId("E2E-T1")
                .uetr("uetr-E2E-T1")
                .money(Money.of(new BigDecimal("1500.00"), "EUR"))
                .debtorIban("DE89370400440532013000")
                .debtorName("ACME")
                .creditorIban("FR7630006000011234567890189")
                .creditorName("Dupont")
                .executionDatetime(LocalDateTime.of(2026, 10, 15, 9, 15))
                .status("COMPLETED")
                .scheme("SCT INST")
                .build();

        // WHEN
        fr.kevin.llps.payments.entity.InstantPayment actual = mapper.toEntity(payment);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.id).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000601"));
        assertThat(actual.endToEndId).isEqualTo("E2E-T1");
        assertThat(actual.amount).isEqualTo(new BigDecimal("1500.00"));
        assertThat(actual.executionDatetime).isEqualTo(Timestamp.valueOf("2026-10-15 09:15:00"));
        assertThat(actual.scheme).isEqualTo("SCT INST");
        assertThat(actual.createdAt).isNull();
    }

    @Test
    void should_map_the_entity_to_the_aggregate() {
        // GIVEN
        fr.kevin.llps.payments.entity.InstantPayment entity = new fr.kevin.llps.payments.entity.InstantPayment();
        entity.id = UUID.fromString("00000000-0000-0000-0000-000000000605");
        entity.endToEndId = "E2E-F1";
        entity.executionDatetime = Timestamp.valueOf("2026-10-15 09:15:00");

        // WHEN
        InstantPayment actual = mapper.toDomain(entity);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.endToEndId()).contains("E2E-F1");
        assertThat(actual.executionDatetime()).contains(LocalDateTime.of(2026, 10, 15, 9, 15));
        assertThat(actual.scheme()).isEmpty();
    }
}
