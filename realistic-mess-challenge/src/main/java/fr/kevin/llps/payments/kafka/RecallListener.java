package fr.kevin.llps.payments.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RecallListener {

    private static final Logger LOG = LoggerFactory.getLogger(RecallListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @KafkaListener(topics = "pay.recall", groupId = "realistic-recall-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            String id = node.path("id").asText();
            String sql = "INSERT INTO recalls (id, recall_reference, recall_type, original_payment_id, original_payment_type, reason_code, amount, currency, initiated_at, deadline, status, created_at, updated_at) VALUES ('"
                    + id + "', '"
                    + node.path("recallReference").asText() + "', '"
                    + node.path("recallType").asText() + "', '"
                    + node.path("originalPaymentId").asText() + "', '"
                    + node.path("originalPaymentType").asText() + "', '"
                    + node.path("reasonCode").asText() + "', "
                    + node.path("amount").decimalValue() + ", '"
                    + node.path("currency").asText() + "', "
                    + timestampOf(node.path("initiatedAt").asText()) + ", "
                    + dateOf(node.path("deadline").asText()) + ", '"
                    + node.path("status").asText() + "', now(), now()) "
                    + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, deadline = EXCLUDED.deadline, updated_at = now()";
            jdbc.execute(sql);
        } catch (Exception e) {
            LOG.warn("could not persist recall event, skipping", e);
        }
    }

    private String timestampOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return "null";
        }
        return "'" + value.replace("T", " ").replace("Z", "") + "'";
    }

    private String dateOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return "null";
        }
        return "'" + value.substring(0, Math.min(10, value.length())) + "'";
    }
}
