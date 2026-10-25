package fr.kevin.llps.payments.kafka;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fr.kevin.llps.payments.entity.OutboundPayment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Consumes outbound payment events. Historical name: this was a "consumer", not a
 * "listener", when it was written, nobody renamed it when the other four were added.
 */
@Component
public class OutboundPaymentMessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundPaymentMessageConsumer.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager em;

    @KafkaListener(topics = "pay.outbound", groupId = "jpa-outbound-group")
    @Transactional
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            OutboundPayment payment = new OutboundPayment();
            payment.id = UUID.fromString(node.path("id").asText());
            payment.instructionId = node.path("instructionId").asText();
            payment.amount = node.path("amount").decimalValue();
            payment.currency = node.path("currency").asText();
            payment.debtorIban = node.path("debtorIban").asText();
            payment.debtorName = node.path("debtorName").asText();
            payment.destinationBic = node.path("destinationBic").asText();
            payment.requestedExecutionDate = dateOf(node.path("requestedExecutionDate").asText());
            payment.chargesType = node.path("chargesType").asText();
            payment.status = node.path("status").asText();
            payment.createdAt = new Timestamp(System.currentTimeMillis());
            payment.updatedAt = new Timestamp(System.currentTimeMillis());
            em.merge(payment);
        } catch (Exception e) {
            LOG.warn("could not persist outbound payment event, skipping", e);
        }
    }

    private Date dateOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        return Date.valueOf(value.substring(0, Math.min(10, value.length())));
    }
}
