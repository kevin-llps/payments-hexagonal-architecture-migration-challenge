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
public class InstantPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(InstantPaymentListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @KafkaListener(topics = "pay.instant", groupId = "realistic-instant-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            String id = node.path("id").asText();
            String sql = "INSERT INTO instant_payments (id, end_to_end_id, uetr, amount, currency, debtor_iban, debtor_name, creditor_iban, creditor_name, execution_datetime, status, scheme, created_at, updated_at) VALUES ('"
                    + id + "', '"
                    + node.path("endToEndId").asText() + "', '"
                    + node.path("uetr").asText() + "', "
                    + node.path("amount").decimalValue() + ", '"
                    + node.path("currency").asText() + "', '"
                    + node.path("debtorIban").asText() + "', '"
                    + node.path("debtorName").asText() + "', '"
                    + node.path("creditorIban").asText() + "', '"
                    + node.path("creditorName").asText() + "', "
                    + timestampOf(node.path("executionDateTime").asText()) + ", '"
                    + node.path("status").asText() + "', '"
                    + node.path("scheme").asText("SCT INST") + "', now(), now()) "
                    + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, updated_at = now()";
            jdbc.execute(sql);
        } catch (Exception e) {
            LOG.warn("could not persist instant payment event, skipping", e);
        }
    }

    private String timestampOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return "null";
        }
        return "'" + value.replace("T", " ").replace("Z", "") + "'";
    }
}
