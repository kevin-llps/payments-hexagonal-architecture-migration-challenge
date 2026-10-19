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
public class InboundPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(InboundPaymentListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @KafkaListener(topics = "pay.inbound", groupId = "realistic-inbound-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            String id = node.path("id").asText();
            String sql = "INSERT INTO inbound_payments (id, credit_transfer_ref, amount, currency, creditor_iban, creditor_name, origin_bic, value_date, settlement_date, status, created_at, updated_at) VALUES ('"
                    + id + "', '"
                    + node.path("creditTransferRef").asText() + "', "
                    + node.path("amount").decimalValue() + ", '"
                    + node.path("currency").asText() + "', '"
                    + node.path("creditorIban").asText() + "', '"
                    + node.path("creditorName").asText() + "', '"
                    + node.path("originBic").asText() + "', "
                    + dateOf(node.path("valueDate").asText()) + ", "
                    + dateOf(node.path("settlementDate").asText()) + ", '"
                    + node.path("status").asText() + "', now(), now()) "
                    + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, settlement_date = EXCLUDED.settlement_date, updated_at = now()";
            jdbc.execute(sql);
        } catch (Exception e) {
            LOG.warn("could not persist inbound payment event, skipping", e);
        }
    }

    private String dateOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return "null";
        }
        return "'" + value.substring(0, Math.min(10, value.length())) + "'";
    }
}
