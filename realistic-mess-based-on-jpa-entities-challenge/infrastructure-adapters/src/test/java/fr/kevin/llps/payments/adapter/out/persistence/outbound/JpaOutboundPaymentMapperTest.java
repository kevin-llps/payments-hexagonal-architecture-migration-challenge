package fr.kevin.llps.payments.adapter.out.persistence.outbound;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.kevin.llps.payments.domain.outbound.OutboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

class JpaOutboundPaymentMapperTest {

    private final JpaOutboundPaymentMapper mapper = new JpaOutboundPaymentMapper();

    @Test
    void should_map_every_attribute_to_the_entity() {
        // GIVEN
        OutboundPayment payment = OutboundPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000401"))
                .instructionId("OUT-2026-9001")
                .money(Money.of(new BigDecimal("780.00"), "EUR"))
                .debtorIban("DE89370400440532013000")
                .debtorName("ACME GmbH")
                .destinationBic("ABNANL2A")
                .requestedExecutionDate(LocalDate.of(2026, 10, 16))
                .chargesType("SHA")
                .status("SENT")
                .createdAt(LocalDateTime.of(2026, 10, 16, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 10, 16, 8, 1))
                .build();

        // WHEN
        fr.kevin.llps.payments.entity.OutboundPayment actual = mapper.toEntity(payment);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.id).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000401"));
        assertThat(actual.requestedExecutionDate).isEqualTo(Date.valueOf("2026-10-16"));
        assertThat(actual.createdAt).isEqualTo(Timestamp.valueOf("2026-10-16 08:00:00"));
        assertThat(actual.updatedAt).isEqualTo(Timestamp.valueOf("2026-10-16 08:01:00"));
        assertThat(actual.chargesType).isEqualTo("SHA");
    }

    @Test
    void should_map_the_entity_to_the_aggregate() {
        // GIVEN
        fr.kevin.llps.payments.entity.OutboundPayment entity = new fr.kevin.llps.payments.entity.OutboundPayment();
        entity.id = UUID.fromString("00000000-0000-0000-0000-000000000634");
        entity.instructionId = "OUT-4";
        entity.requestedExecutionDate = Date.valueOf("2026-10-18");

        // WHEN
        OutboundPayment actual = mapper.toDomain(entity);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.instructionId()).contains("OUT-4");
        assertThat(actual.requestedExecutionDate()).contains(LocalDate.of(2026, 10, 18));
        assertThat(actual.money().amount()).isEmpty();
    }
}
