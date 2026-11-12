package fr.kevin.llps.payments.adapter.in.kafka.outbound;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.kevin.llps.payments.domain.outbound.command.PersistOutboundPaymentCommand;
import fr.kevin.llps.payments.domain.outbound.port.in.PersistOutboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Consumes {@code pay.outbound} with the legacy parsing grammar (BR-ING-01..09, BR-ING-12); renamed from the legacy
 * {@code OutboundPaymentMessageConsumer}. Any parsing or persisting failure is logged and the event skipped
 * (BR-ING-05); commit-time violations still propagate (BR-ING-06).
 */
@Component
public class KafkaOutboundPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaOutboundPaymentListener.class);
    private static final int DATE_LENGTH = 10;
    private static final String NULL_LITERAL = "null";

    // A private mapper, not Spring's, so that number parsing cannot drift from the legacy listener (TD-11).
    private final ObjectMapper mapper = new ObjectMapper();
    private final PersistOutboundPayment persistOutboundPayment;

    public KafkaOutboundPaymentListener(PersistOutboundPayment persistOutboundPayment) {
        this.persistOutboundPayment = persistOutboundPayment;
    }

    @KafkaListener(topics = "pay.outbound", groupId = "jpa-outbound-group")
    @Transactional
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            persistOutboundPayment.persist(new PersistOutboundPaymentCommand(
                    PaymentId.of(UUID.fromString(node.path("id").asText())),
                    node.path("instructionId").asText(),
                    node.path("amount").decimalValue(),
                    node.path("currency").asText(),
                    node.path("debtorIban").asText(),
                    node.path("debtorName").asText(),
                    node.path("destinationBic").asText(),
                    dateOf(node.path("requestedExecutionDate").asText()),
                    node.path("chargesType").asText(),
                    node.path("status").asText()));
        } catch (Exception e) {
            LOG.warn("could not persist outbound payment event, skipping", e);
        }
    }

    private static LocalDate dateOf(String value) {
        if (value == null || value.isBlank() || NULL_LITERAL.equals(value)) {
            return null;
        }
        return Date.valueOf(value.substring(0, Math.min(DATE_LENGTH, value.length()))).toLocalDate();
    }
}
