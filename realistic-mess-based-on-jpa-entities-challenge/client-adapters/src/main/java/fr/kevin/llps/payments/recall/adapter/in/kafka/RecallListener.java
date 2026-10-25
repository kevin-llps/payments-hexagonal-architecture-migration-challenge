package fr.kevin.llps.payments.recall.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kevin.llps.payments.recall.port.in.RecordRecallCommand;
import fr.kevin.llps.payments.recall.port.in.RecordRecallUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class RecallListener {

    private static final Logger LOG = LoggerFactory.getLogger(RecallListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final RecordRecallUseCase recordRecall;

    public RecallListener(RecordRecallUseCase recordRecall) {
        this.recordRecall = recordRecall;
    }

    @KafkaListener(topics = "pay.recall", groupId = "jpa-recall-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            recordRecall.record(new RecordRecallCommand(
                    node.path("id").asText(),
                    node.path("recallReference").asText(),
                    node.path("recallType").asText(),
                    node.path("originalPaymentId").asText(),
                    node.path("originalPaymentType").asText(),
                    node.path("reasonCode").asText(),
                    node.path("amount").decimalValue(),
                    node.path("currency").asText(),
                    timestampOf(node.path("initiatedAt").asText()),
                    dateOf(node.path("deadline").asText()),
                    node.path("status").asText()));
        } catch (Exception e) {
            LOG.warn("could not persist recall event, skipping", e);
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

    private LocalDate dateOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        return Date.valueOf(value.substring(0, Math.min(10, value.length()))).toLocalDate();
    }
}
