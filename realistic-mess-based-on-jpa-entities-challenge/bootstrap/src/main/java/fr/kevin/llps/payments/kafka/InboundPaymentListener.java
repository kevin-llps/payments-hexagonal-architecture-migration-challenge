package fr.kevin.llps.payments.kafka;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fr.kevin.llps.payments.entity.InboundPayment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class InboundPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(InboundPaymentListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager em;

    @KafkaListener(topics = "pay.inbound", groupId = "jpa-inbound-group")
    @Transactional
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            InboundPayment payment = new InboundPayment();
            payment.id = UUID.fromString(node.path("id").asText());
            payment.creditTransferRef = node.path("creditTransferRef").asText();
            payment.amount = node.path("amount").decimalValue();
            payment.currency = node.path("currency").asText();
            payment.creditorIban = node.path("creditorIban").asText();
            payment.creditorName = node.path("creditorName").asText();
            payment.originBic = node.path("originBic").asText();
            payment.valueDate = dateOf(node.path("valueDate").asText());
            payment.settlementDate = dateOf(node.path("settlementDate").asText());
            payment.status = node.path("status").asText();
            payment.createdAt = new Timestamp(System.currentTimeMillis());
            payment.updatedAt = new Timestamp(System.currentTimeMillis());
            em.merge(payment);
        } catch (Exception e) {
            LOG.warn("could not persist inbound payment event, skipping", e);
        }
    }

    private Date dateOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        return Date.valueOf(value.substring(0, Math.min(10, value.length())));
    }
}
