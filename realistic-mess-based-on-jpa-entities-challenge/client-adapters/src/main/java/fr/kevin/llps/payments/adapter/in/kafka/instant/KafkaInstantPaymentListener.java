package fr.kevin.llps.payments.adapter.in.kafka.instant;

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

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.instant.command.PersistInstantPaymentCommand;
import fr.kevin.llps.payments.domain.instant.port.in.PersistInstantPayment;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

/**
 * Consumes {@code pay.instant} with the legacy parsing grammar (BR-ING-01..10, BR-ING-12): the event field is
 * {@code executionDateTime}, a missing or null {@code scheme} becomes {@link InstantPayment#DEFAULT_SCHEME}. Any
 * parsing or persisting failure is logged and the event skipped (BR-ING-05); commit-time violations still propagate
 * (BR-ING-06).
 */
@Component
public class KafkaInstantPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaInstantPaymentListener.class);
    private static final int DATE_ONLY_LENGTH = 10;
    private static final String NULL_LITERAL = "null";

    // A private mapper, not Spring's, so that number parsing cannot drift from the legacy listener (TD-11).
    private final ObjectMapper mapper = new ObjectMapper();
    private final PersistInstantPayment persistInstantPayment;

    public KafkaInstantPaymentListener(PersistInstantPayment persistInstantPayment) {
        this.persistInstantPayment = persistInstantPayment;
    }

    @KafkaListener(topics = "pay.instant", groupId = "jpa-instant-group")
    @Transactional
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            persistInstantPayment.persist(new PersistInstantPaymentCommand(
                    PaymentId.of(UUID.fromString(node.path("id").asText())),
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
                    node.path("scheme").asText(InstantPayment.DEFAULT_SCHEME)));
        } catch (Exception e) {
            LOG.warn("could not persist instant payment event, skipping", e);
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
