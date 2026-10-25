package fr.kevin.llps.payments.processing.inbound.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentCommand;
import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;

@Component
public class InboundPaymentListener {

    private static final Logger LOG = LoggerFactory.getLogger(InboundPaymentListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final RecordInboundPaymentUseCase recordInboundPayment;

    public InboundPaymentListener(RecordInboundPaymentUseCase recordInboundPayment) {
        this.recordInboundPayment = recordInboundPayment;
    }

    @KafkaListener(topics = "pay.inbound", groupId = "jpa-inbound-group")
    public void onMessage(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            recordInboundPayment.record(new RecordInboundPaymentCommand(
                    node.path("id").asText(),
                    node.path("creditTransferRef").asText(),
                    node.path("amount").decimalValue(),
                    node.path("currency").asText(),
                    node.path("creditorIban").asText(),
                    node.path("creditorName").asText(),
                    node.path("originBic").asText(),
                    dateOf(node.path("valueDate").asText()),
                    dateOf(node.path("settlementDate").asText()),
                    node.path("status").asText()));
        } catch (Exception e) {
            LOG.warn("could not persist inbound payment event, skipping", e);
        }
    }

    private LocalDate dateOf(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        return Date.valueOf(value.substring(0, Math.min(10, value.length()))).toLocalDate();
    }
}
