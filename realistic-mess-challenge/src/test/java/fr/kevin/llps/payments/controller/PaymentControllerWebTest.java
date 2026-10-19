package fr.kevin.llps.payments.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.dto.RecallResponse;
import fr.kevin.llps.payments.dto.SuspendedPaymentResponse;
import fr.kevin.llps.payments.service.InboundPaymentService;
import fr.kevin.llps.payments.service.InstantPaymentService;
import fr.kevin.llps.payments.service.OutboundPaymentService;
import fr.kevin.llps.payments.service.RecallService;
import fr.kevin.llps.payments.service.SuspendedPaymentService;

@WebMvcTest(controllers = {
        InstantPaymentController.class,
        SuspendedPaymentController.class,
        InboundPaymentController.class,
        OutboundPaymentController.class,
        RecallController.class
})
class PaymentControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InstantPaymentService instantService;
    @MockitoBean
    private SuspendedPaymentService suspendedService;
    @MockitoBean
    private InboundPaymentService inboundService;
    @MockitoBean
    private OutboundPaymentService outboundService;
    @MockitoBean
    private RecallService recallService;
    @MockitoBean
    private JdbcTemplate jdbc;

    // ---------------- instant ----------------

    @Test
    void instantSearch_returnsRows() throws Exception {
        InstantPaymentResponse response = new InstantPaymentResponse();
        response.id = "1";
        when(instantService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/instant-payments/search").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    @Test
    void instantSearch_serviceFailure_returns500() throws Exception {
        when(instantService.search(any())).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/v1/instant-payments/search"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void instantDetails_foundAndMissing() throws Exception {
        InstantPaymentResponse response = new InstantPaymentResponse();
        response.id = "1";
        when(instantService.getById("1")).thenReturn(response);
        when(instantService.getById("missing")).thenReturn(null);

        mvc.perform(get("/api/v1/instant-payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
        mvc.perform(get("/api/v1/instant-payments/missing"))
                .andExpect(status().isNotFound());
    }

    // ---------------- suspended ----------------

    @Test
    void suspendedSearch_returnsRows() throws Exception {
        SuspendedPaymentResponse response = new SuspendedPaymentResponse();
        response.id = "s1";
        when(suspendedService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/suspended-payments/search").param("reviewStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("s1"));
    }

    @Test
    void suspendedDetails_foundAndMissing() throws Exception {
        SuspendedPaymentResponse response = new SuspendedPaymentResponse();
        response.id = "s1";
        when(suspendedService.getById("s1")).thenReturn(response);
        when(suspendedService.getById("missing")).thenReturn(null);

        mvc.perform(get("/api/v1/suspended-payments/s1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/suspended-payments/missing")).andExpect(status().isNotFound());
    }

    // ---------------- inbound ----------------

    @Test
    void inboundSearch_computesAgeInDays() throws Exception {
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        response.valueDate = LocalDate.now().minusDays(2).toString();
        when(inboundService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageInDays").value(2));
    }

    @Test
    void inboundDetails_foundAndMissing() throws Exception {
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        when(inboundService.getById("i1")).thenReturn(response);
        when(inboundService.getById("missing")).thenReturn(null);

        mvc.perform(get("/api/v1/inbound-payments/i1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/inbound-payments/missing")).andExpect(status().isNotFound());
    }

    // ---------------- outbound ----------------

    @Test
    void outboundSearch_returnsRows() throws Exception {
        OutboundPaymentResponse response = new OutboundPaymentResponse();
        response.id = "o1";
        when(outboundService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/outbound-payments/search").param("chargesType", "OUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("o1"));
    }

    @Test
    void outboundDetails_computesFeeFromController() throws Exception {
        Map<String, Object> row = new HashMap<>();
        row.put("id", "o1");
        row.put("amount", new BigDecimal("1000.00"));
        row.put("charges_type", "OUR");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(row);

        mvc.perform(get("/api/v1/outbound-payments/o1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFee").value(20.00));
    }

    @Test
    void outboundDetails_missing_returns404() throws Exception {
        when(jdbc.queryForMap(anyString(), any(Object[].class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        mvc.perform(get("/api/v1/outbound-payments/missing")).andExpect(status().isNotFound());
    }

    @Test
    void outboundDetails_dbFailure_returns500() throws Exception {
        when(jdbc.queryForMap(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("db down"));

        mvc.perform(get("/api/v1/outbound-payments/o1")).andExpect(status().isInternalServerError());
    }

    // ---------------- recalls ----------------

    @Test
    void recallsSearch_returnsRows() throws Exception {
        RecallResponse response = new RecallResponse();
        response.id = "r1";
        when(recallService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/recalls/search").param("recallType", "R-COR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("r1"));
    }

    @Test
    void recallsSearch_invalidType_returns400() throws Exception {
        when(recallService.search(any())).thenThrow(new IllegalArgumentException("Unknown recall type: NOPE"));

        mvc.perform(get("/api/v1/recalls/search").param("recallType", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown recall type: NOPE"));
    }

    @Test
    void recallsDetails_foundAndMissing() throws Exception {
        RecallResponse response = new RecallResponse();
        response.id = "r1";
        when(recallService.getById("r1")).thenReturn(response);
        when(recallService.getById("missing")).thenReturn(null);

        mvc.perform(get("/api/v1/recalls/r1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/recalls/missing")).andExpect(status().isNotFound());
    }
}
