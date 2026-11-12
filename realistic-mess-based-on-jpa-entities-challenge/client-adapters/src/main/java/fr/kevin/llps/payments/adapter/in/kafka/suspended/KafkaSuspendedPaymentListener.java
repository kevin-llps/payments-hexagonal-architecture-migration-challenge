package fr.kevin.llps.payments.adapter.in.kafka.suspended;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.command.RecordSuspendedPaymentCommand;
import fr.kevin.llps.payments.domain.suspended.port.in.RecordSuspendedPayment;

/**
 * Consumes {@code pay.suspended} with the legacy parsing grammar (BR-ING-01..09, BR-ING-12). Any parsing or
 * recording failure is logged and the event skipped (BR-ING-05); the transaction commits after the {@code try}, so
 * commit-time violations still propagate (BR-ING-06).
 */
@Component
public class KafkaSuspendedPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaSuspendedPaymentListener.class);
    private static final int DATE_ONLY_LENGTH = 10;
    private static final String NULL_LITERAL = "null";

    // A private mapper, not Spring's, so that number parsing cannot drift from the legacy listener (TD-11).
    private final ObjectMapper mapper = new ObjectMapper();
    private final RecordSuspendedPayment recordSuspendedPayment;

    public KafkaSuspendedPaymentListener(RecordSuspendedPayment recordSuspendedPayment) {
        this.recordSuspendedPayment = recordSuspendedPayment;
    }

    @KafkaListener(topics = "pay.suspended", groupId = "jpa-suspended-group")
    @Transactional
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            recordSuspendedPayment.record(new RecordSuspendedPaymentCommand(
                    PaymentId.of(UUID.fromString(node.path("id").asText())),
                    node.path("amount").decimalValue(),
                    node.path("currency").asText(),
                    node.path("debtorIban").asText(),
                    node.path("debtorName").asText(),
                    node.path("creditorIban").asText(),
                    node.path("creditorName").asText(),
                    node.path("holdReason").asText(),
                    node.path("complianceCaseRef").asText(),
                    node.path("reviewStatus").asText(),
                    timestampOf(node.path("heldAt").asText()),
                    timestampOf(node.path("releasedAt").asText())));
        } catch (Exception e) {
            LOG.warn("could not persist suspended payment event, skipping", e);
        }
    }

    private static LocalDateTime timestampOf(String value) {
        if (value == null || value.isBlank() || NULL_LITERAL.equals(value)) {
            return null;
        }
        String text = value.replace("T", " ").replace("Z", "");
        if (text.length() == DATE_ONLY_LENGTH) {
            text = text + " 00:00:00";
        }
        return Timestamp.valueOf(text).toLocalDateTime();
    }
}
