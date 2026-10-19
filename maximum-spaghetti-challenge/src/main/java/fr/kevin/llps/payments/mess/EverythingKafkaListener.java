package fr.kevin.llps.payments.mess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * All five topics, one class, because "the god service already handles everything".
 */
@Component
public class EverythingKafkaListener {

    private static final Logger LOG = LoggerFactory.getLogger(EverythingKafkaListener.class);

    @Autowired
    private PaymentsGodService godService;

    @KafkaListener(topics = "pay.instant", groupId = "spaghetti-consumer")
    public void onInstant(String raw) {
        try {
            godService.ingest("pay.instant", raw);
        } catch (Exception e) {
            LOG.error("swallowing error for pay.instant", e);
        }
    }

    @KafkaListener(topics = "pay.suspended", groupId = "spaghetti-consumer")
    public void onSuspended(String raw) {
        try {
            godService.ingest("pay.suspended", raw);
        } catch (Exception e) {
            LOG.error("swallowing error for pay.suspended", e);
        }
    }

    @KafkaListener(topics = "pay.inbound", groupId = "spaghetti-consumer")
    public void onInbound(String raw) {
        try {
            godService.ingest("pay.inbound", raw);
        } catch (Exception e) {
            LOG.error("swallowing error for pay.inbound", e);
        }
    }

    @KafkaListener(topics = "pay.outbound", groupId = "spaghetti-consumer")
    public void onOutbound(String raw) {
        try {
            godService.ingest("pay.outbound", raw);
        } catch (Exception e) {
            LOG.error("swallowing error for pay.outbound", e);
        }
    }

    @KafkaListener(topics = "pay.recall", groupId = "spaghetti-consumer")
    public void onRecall(String raw) {
        try {
            godService.ingest("pay.recall", raw);
        } catch (Exception e) {
            LOG.error("swallowing error for pay.recall", e);
        }
    }
}
