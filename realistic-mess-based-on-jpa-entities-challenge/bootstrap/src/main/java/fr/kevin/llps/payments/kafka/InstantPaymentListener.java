package fr.kevin.llps.payments.kafka;

import java.sql.Timestamp;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fr.kevin.llps.payments.entity.InstantPayment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class InstantPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(InstantPaymentListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager em;

    @KafkaListener(topics = "pay.instant", groupId = "jpa-instant-group")
    @Transactional
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            InstantPayment payment = new InstantPayment();
            payment.id = UUID.fromString(node.path("id").asText());
            payment.endToEndId = node.path("endToEndId").asText();
            payment.uetr = node.path("uetr").asText();
            payment.amount = node.path("amount").decimalValue();
            payment.currency = node.path("currency").asText();
            payment.debtorIban = node.path("debtorIban").asText();
            payment.debtorName = node.path("debtorName").asText();
            payment.creditorIban = node.path("creditorIban").asText();
            payment.creditorName = node.path("creditorName").asText();
            payment.executionDatetime = timestampOf(node.path("executionDateTime").asText());
            payment.status = node.path("status").asText();
            payment.scheme = node.path("scheme").asText("SCT INST");
            payment.createdAt = new Timestamp(System.currentTimeMillis());
            payment.updatedAt = new Timestamp(System.currentTimeMillis());
            em.merge(payment);
        } catch (Exception e) {
            LOG.warn("could not persist instant payment event, skipping", e);
        }
    }

    private Timestamp timestampOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        String s = value.replace("T", " ").replace("Z", "");
        if (s.length() == 10) {
            s = s + " 00:00:00";
        }
        return Timestamp.valueOf(s);
    }
}
