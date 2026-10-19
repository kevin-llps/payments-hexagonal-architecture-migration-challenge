package fr.kevin.llps.payments.mess;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EverythingKafkaListenerUnitTest {

    @Mock
    private PaymentsGodService godService;

    @InjectMocks
    private EverythingKafkaListener listener;

    @Test
    void onInstant_delegatesToIngest() {
        listener.onInstant("{\"id\":\"1\"}");
        verify(godService).ingest(eq("pay.instant"), eq("{\"id\":\"1\"}"));
    }

    @Test
    void onSuspended_delegatesToIngest() {
        listener.onSuspended("{\"id\":\"2\"}");
        verify(godService).ingest(eq("pay.suspended"), eq("{\"id\":\"2\"}"));
    }

    @Test
    void onInbound_delegatesToIngest() {
        listener.onInbound("{\"id\":\"3\"}");
        verify(godService).ingest(eq("pay.inbound"), eq("{\"id\":\"3\"}"));
    }

    @Test
    void onOutbound_delegatesToIngest() {
        listener.onOutbound("{\"id\":\"4\"}");
        verify(godService).ingest(eq("pay.outbound"), eq("{\"id\":\"4\"}"));
    }

    @Test
    void onRecall_delegatesToIngest() {
        listener.onRecall("{\"id\":\"5\"}");
        verify(godService).ingest(eq("pay.recall"), eq("{\"id\":\"5\"}"));
    }

    @Test
    void onInstant_swallowsServiceExceptions() {
        doThrow(new RuntimeException("boom")).when(godService).ingest(eq("pay.instant"), eq("bad"));

        listener.onInstant("bad");

        verify(godService).ingest(eq("pay.instant"), eq("bad"));
    }
}
