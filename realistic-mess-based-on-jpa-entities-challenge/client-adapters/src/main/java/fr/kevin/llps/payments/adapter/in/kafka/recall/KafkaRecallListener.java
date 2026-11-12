package fr.kevin.llps.payments.adapter.in.kafka.recall;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.kevin.llps.payments.domain.recall.command.PersistRecallCommand;
import fr.kevin.llps.payments.domain.recall.port.in.PersistRecall;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Consumes {@code pay.recall} with the legacy parsing grammar (BR-ING-01..09, BR-ING-12). An event whose
 * {@code originalPaymentId} is not a UUID is dropped (BR-ING-11); any parsing or persisting failure is logged and the
 * event skipped (BR-ING-05); commit-time violations still propagate (BR-ING-06).
 */
@Component
public class KafkaRecallListener {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaRecallListener.class);
    private static final int DATE_ONLY_LENGTH = 10;
    private static final String NULL_LITERAL = "null";

    // A private mapper, not Spring's, so that number parsing cannot drift from the legacy listener (TD-11).
    private final ObjectMapper mapper = new ObjectMapper();
    private final PersistRecall persistRecall;

    public KafkaRecallListener(PersistRecall persistRecall) {
        this.persistRecall = persistRecall;
    }

    @KafkaListener(topics = "pay.recall", groupId = "jpa-recall-group")
    @Transactional
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            persistRecall.persist(new PersistRecallCommand(
                    PaymentId.of(UUID.fromString(node.path("id").asText())),
                    node.path("recallReference").asText(),
                    node.path("recallType").asText(),
                    PaymentId.of(UUID.fromString(node.path("originalPaymentId").asText())),
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

    private static LocalDate dateOf(String value) {
        if (value == null || value.isBlank() || NULL_LITERAL.equals(value)) {
            return null;
        }
        return Date.valueOf(value.substring(0, Math.min(DATE_ONLY_LENGTH, value.length()))).toLocalDate();
    }
}
