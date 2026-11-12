package fr.kevin.llps.payments.kafka;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fr.kevin.llps.payments.entity.Recall;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class RecallListener {

    private static final Logger LOG = LoggerFactory.getLogger(RecallListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager em;

    @KafkaListener(topics = "pay.recall", groupId = "jpa-recall-group")
    @Transactional
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            Recall recall = new Recall();
            recall.id = UUID.fromString(node.path("id").asText());
            recall.recallReference = node.path("recallReference").asText();
            recall.recallType = node.path("recallType").asText();
            recall.originalPaymentId = UUID.fromString(node.path("originalPaymentId").asText());
            recall.originalPaymentType = node.path("originalPaymentType").asText();
            recall.reasonCode = node.path("reasonCode").asText();
            recall.amount = node.path("amount").decimalValue();
            recall.currency = node.path("currency").asText();
            recall.initiatedAt = timestampOf(node.path("initiatedAt").asText());
            recall.deadline = dateOf(node.path("deadline").asText());
            recall.status = node.path("status").asText();
            recall.createdAt = new Timestamp(System.currentTimeMillis());
            recall.updatedAt = new Timestamp(System.currentTimeMillis());
            em.merge(recall);
        } catch (Exception e) {
            LOG.warn("could not persist recall event, skipping", e);
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

    private Date dateOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        return Date.valueOf(value.substring(0, Math.min(10, value.length())));
    }
}
