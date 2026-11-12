package fr.kevin.llps.payments.adapter.in.kafka.inbound;

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

import fr.kevin.llps.payments.domain.inbound.command.PersistInboundPaymentCommand;
import fr.kevin.llps.payments.domain.inbound.port.in.PersistInboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Consumes {@code pay.inbound} with the legacy parsing grammar (BR-ING-01..09, BR-ING-12): dates are read from their
 * first ten characters. Any parsing or persisting failure is logged and the event skipped (BR-ING-05); commit-time
 * violations still propagate (BR-ING-06).
 */
@Component
public class KafkaInboundPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaInboundPaymentListener.class);
    private static final int DATE_LENGTH = 10;
    private static final String NULL_LITERAL = "null";

    // A private mapper, not Spring's, so that number parsing cannot drift from the legacy listener (TD-11).
    private final ObjectMapper mapper = new ObjectMapper();
    private final PersistInboundPayment persistInboundPayment;

    public KafkaInboundPaymentListener(PersistInboundPayment persistInboundPayment) {
        this.persistInboundPayment = persistInboundPayment;
    }

    @KafkaListener(topics = "pay.inbound", groupId = "jpa-inbound-group")
    @Transactional
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            persistInboundPayment.persist(new PersistInboundPaymentCommand(
                    PaymentId.of(UUID.fromString(node.path("id").asText())),
                    node.path("creditTransferRef").asText(),
                    node.path("amount").decimalValue(),
                    node.path("currency").asText(),
                    node.path("creditorIban").asText(),
                    node.path("creditorName").asText(),
                    node.path("originBic").asText(),
                    dateOf(node.path("valueDate").asText()),
                    dateOf(node.path("settlementDate").asText()),
                    node.path("status").asText()));
        } catch (Exception e) {
            LOG.warn("could not persist inbound payment event, skipping", e);
        }
    }

    private static LocalDate dateOf(String value) {
        if (value == null || value.isBlank() || NULL_LITERAL.equals(value)) {
            return null;
        }
        return Date.valueOf(value.substring(0, Math.min(DATE_LENGTH, value.length()))).toLocalDate();
    }
}
