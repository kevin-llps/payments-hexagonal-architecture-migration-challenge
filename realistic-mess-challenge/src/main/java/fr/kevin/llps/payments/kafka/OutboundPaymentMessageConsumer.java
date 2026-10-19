package fr.kevin.llps.payments.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Consumes outbound payment events. Historical name: this was a "consumer", not a
 * "listener", when it was written, nobody renamed it when the other four were added.
 */
@Component
public class OutboundPaymentMessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundPaymentMessageConsumer.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @KafkaListener(topics = "pay.outbound", groupId = "realistic-outbound-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            String id = node.path("id").asText();
            String sql = "INSERT INTO outbound_payments (id, instruction_id, amount, currency, debtor_iban, debtor_name, destination_bic, requested_execution_date, charges_type, status, created_at, updated_at) VALUES ('"
                    + id + "', '"
                    + node.path("instructionId").asText() + "', "
                    + node.path("amount").decimalValue() + ", '"
                    + node.path("currency").asText() + "', '"
                    + node.path("debtorIban").asText() + "', '"
                    + node.path("debtorName").asText() + "', '"
                    + node.path("destinationBic").asText() + "', "
                    + dateOf(node.path("requestedExecutionDate").asText()) + ", '"
                    + node.path("chargesType").asText() + "', '"
                    + node.path("status").asText() + "', now(), now()) "
                    + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, updated_at = now()";
            jdbc.execute(sql);
        } catch (Exception e) {
            LOG.warn("could not persist outbound payment event, skipping", e);
        }
    }

    private String dateOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return "null";
        }
        return "'" + value.substring(0, Math.min(10, value.length())) + "'";
    }
}
