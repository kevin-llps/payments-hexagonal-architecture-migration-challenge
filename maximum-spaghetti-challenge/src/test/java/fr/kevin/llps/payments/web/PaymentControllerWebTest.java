package fr.kevin.llps.payments.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.mess.PaymentsGodService;

@WebMvcTest(PaymentController.class)
class PaymentControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentsGodService godService;

    @Test
    void instantSearch_forwardsToService() throws Exception {
        when(godService.searchInstantPayments(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(Map.of("id", "1", "status", "PENDING")));

        mockMvc.perform(get("/api/v1/instant-payments/search").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void outboundSearch_forwardsToService() throws Exception {
        when(godService.searchOutboundPayments(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(Map.of("id", "1", "estimatedFee", 20.00)));

        mockMvc.perform(get("/api/v1/outbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estimatedFee").value(20.00));
    }

    @Test
    void recallsSearch_convertsNegativeDaysLeftToPositive() throws Exception {
        Map<String, Object> recall = new HashMap<>();
        recall.put("id", "1");
        recall.put("daysLeft", -3L);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(recall);
        when(godService.searchRecalls(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(rows);

        mockMvc.perform(get("/api/v1/recalls/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].daysLeft").value(3));
    }

    @Test
    void details_returnsPayment() throws Exception {
        when(godService.getPaymentDetails("instant", "1")).thenReturn(Map.of("type", "INSTANT", "id", "1"));

        mockMvc.perform(get("/api/v1/payments/instant/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INSTANT"));
    }

    @Test
    void details_missing_returns404() throws Exception {
        when(godService.getPaymentDetails("instant", "nope")).thenReturn(null);

        mockMvc.perform(get("/api/v1/payments/instant/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("payment not found"));
    }

    @Test
    void details_unknownType_returns400() throws Exception {
        when(godService.getPaymentDetails("wat", "1"))
                .thenThrow(new IllegalArgumentException("unknown payment type: wat"));

        mockMvc.perform(get("/api/v1/payments/wat/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unknown payment type: wat"));
    }

    @Test
    void export_returnsCsv() throws Exception {
        when(godService.exportLegacyCsv("recall")).thenReturn("id;status;\nr1;INITIATED;\n");

        mockMvc.perform(get("/api/v1/payments/export").param("type", "recall"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(content().string("id;status;\nr1;INITIATED;\n"));
    }

    @Test
    void export_failure_returns500() throws Exception {
        when(godService.exportLegacyCsv("instant")).thenReturn("ERROR");

        mockMvc.perform(get("/api/v1/payments/export").param("type", "instant"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void stats_returnsInternalStats() throws Exception {
        when(godService.getInternalStats()).thenReturn(Map.of("cacheSize", 2));

        mockMvc.perform(get("/api/v1/internal/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheSize").value(2));
    }

    @Test
    void search_serviceCrash_returns500() throws Exception {
        when(godService.searchInstantPayments(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/instant-payments/search"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal error: RuntimeException"));
    }
}
