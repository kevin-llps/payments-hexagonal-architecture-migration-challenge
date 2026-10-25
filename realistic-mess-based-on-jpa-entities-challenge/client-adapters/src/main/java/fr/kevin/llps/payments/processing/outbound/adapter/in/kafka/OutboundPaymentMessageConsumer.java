package fr.kevin.llps.payments.processing.outbound.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kevin.llps.payments.processing.outbound.port.in.RecordOutboundPaymentCommand;
import fr.kevin.llps.payments.processing.outbound.port.in.RecordOutboundPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Consumes outbound payment events. Historical name: this was a "consumer", not a
 * "listener", when it was written, nobody renamed it when the other four were added.
 */
@Component
public class OutboundPaymentMessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundPaymentMessageConsumer.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final RecordOutboundPaymentUseCase recordOutboundPayment;

    public OutboundPaymentMessageConsumer(RecordOutboundPaymentUseCase recordOutboundPayment) {
        this.recordOutboundPayment = recordOutboundPayment;
    }

    @KafkaListener(topics = "pay.outbound", groupId = "jpa-outbound-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            recordOutboundPayment.record(new RecordOutboundPaymentCommand(
                    node.path("id").asText(),
                    node.path("instructionId").asText(),
                    node.path("amount").decimalValue(),
                    node.path("currency").asText(),
                    node.path("debtorIban").asText(),
                    node.path("debtorName").asText(),
                    node.path("destinationBic").asText(),
                    dateOf(node.path("requestedExecutionDate").asText()),
                    node.path("chargesType").asText(),
                    node.path("status").asText()));
        } catch (Exception e) {
            LOG.warn("could not persist outbound payment event, skipping", e);
        }
    }

    private LocalDate dateOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        return Date.valueOf(value.substring(0, Math.min(10, value.length()))).toLocalDate();
    }
}
