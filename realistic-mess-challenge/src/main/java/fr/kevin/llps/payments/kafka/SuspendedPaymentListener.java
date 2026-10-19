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
public class SuspendedPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(SuspendedPaymentListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @KafkaListener(topics = "pay.suspended", groupId = "realistic-suspended-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            String id = node.path("id").asText();
            String sql = "INSERT INTO suspended_payments (id, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, hold_reason, compliance_case_ref, review_status, held_at, released_at, created_at, updated_at) VALUES ('"
                    + id + "', "
                    + node.path("amount").decimalValue() + ", '"
                    + node.path("currency").asText() + "', '"
                    + node.path("debtorIban").asText() + "', '"
                    + node.path("debtorName").asText() + "', '"
                    + node.path("creditorIban").asText() + "', '"
                    + node.path("creditorName").asText() + "', '"
                    + node.path("holdReason").asText() + "', '"
                    + node.path("complianceCaseRef").asText() + "', '"
                    + node.path("reviewStatus").asText() + "', "
                    + timestampOf(node.path("heldAt").asText()) + ", "
                    + timestampOf(node.path("releasedAt").asText()) + ", now(), now()) "
                    + "ON CONFLICT (id) DO UPDATE SET review_status = EXCLUDED.review_status, released_at = EXCLUDED.released_at, updated_at = now()";
            jdbc.execute(sql);
        } catch (Exception e) {
            LOG.warn("could not persist suspended payment event, skipping", e);
        }
    }

    private String timestampOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return "null";
        }
        return "'" + value.replace("T", " ").replace("Z", "") + "'";
    }
}
