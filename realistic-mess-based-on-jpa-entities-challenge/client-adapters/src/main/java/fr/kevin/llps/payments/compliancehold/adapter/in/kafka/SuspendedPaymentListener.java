package fr.kevin.llps.payments.compliancehold.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentCommand;
import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Component
public class SuspendedPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(SuspendedPaymentListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final RecordSuspendedPaymentUseCase recordSuspendedPayment;

    public SuspendedPaymentListener(RecordSuspendedPaymentUseCase recordSuspendedPayment) {
        this.recordSuspendedPayment = recordSuspendedPayment;
    }

    @KafkaListener(topics = "pay.suspended", groupId = "jpa-suspended-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            recordSuspendedPayment.record(new RecordSuspendedPaymentCommand(
                    node.path("id").asText(),
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

    private LocalDateTime timestampOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        String s = value.replace("T", " ").replace("Z", "");
        if (s.length() == 10) {
            s = s + " 00:00:00";
        }
        return Timestamp.valueOf(s).toLocalDateTime();
    }
}
