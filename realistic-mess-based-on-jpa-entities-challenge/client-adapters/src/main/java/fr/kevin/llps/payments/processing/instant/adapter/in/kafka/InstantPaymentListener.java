package fr.kevin.llps.payments.processing.instant.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kevin.llps.payments.processing.instant.port.in.RecordInstantPaymentCommand;
import fr.kevin.llps.payments.processing.instant.port.in.RecordInstantPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Component
public class InstantPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(InstantPaymentListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final RecordInstantPaymentUseCase recordInstantPayment;

    public InstantPaymentListener(RecordInstantPaymentUseCase recordInstantPayment) {
        this.recordInstantPayment = recordInstantPayment;
    }

    @KafkaListener(topics = "pay.instant", groupId = "jpa-instant-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            recordInstantPayment.record(new RecordInstantPaymentCommand(
                    node.path("id").asText(),
                    node.path("endToEndId").asText(),
                    node.path("uetr").asText(),
                    node.path("amount").decimalValue(),
                    node.path("currency").asText(),
                    node.path("debtorIban").asText(),
                    node.path("debtorName").asText(),
                    node.path("creditorIban").asText(),
                    node.path("creditorName").asText(),
                    timestampOf(node.path("executionDateTime").asText()),
                    node.path("status").asText(),
                    // null when missing or JSON null: the domain applies the default scheme
                    node.path("scheme").asText(null)));
        } catch (Exception e) {
            LOG.warn("could not persist instant payment event, skipping", e);
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
