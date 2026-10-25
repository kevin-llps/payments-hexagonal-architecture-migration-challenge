package fr.kevin.llps.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"pay.instant", "pay.suspended", "pay.inbound", "pay.outbound", "pay.recall"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Testcontainers
class PaymentApplicationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private KafkaTemplate<String, String> kafka;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanRowsCreatedByKafkaTests() {
        jdbc.execute("DELETE FROM instant_payments WHERE id::text LIKE '00000000-0000-0000-0000-0000000009%'");
        jdbc.execute("DELETE FROM suspended_payments WHERE id::text LIKE '00000000-0000-0000-0000-0000000009%'");
        jdbc.execute("DELETE FROM inbound_payments WHERE id::text LIKE '00000000-0000-0000-0000-0000000009%'");
        jdbc.execute("DELETE FROM outbound_payments WHERE id::text LIKE '00000000-0000-0000-0000-0000000009%'");
        jdbc.execute("DELETE FROM recalls WHERE id::text LIKE '00000000-0000-0000-0000-0000000009%'");
    }

    // ---------------- REST search ----------------

    @Test
    void instantSearch_returnsSeededPayments() {
        List<Map<String, Object>> rows = getList("/api/v1/instant-payments/search");
        assertThat(rows).hasSize(3);
    }

    @Test
    void instantSearch_appliesStatusFilter() {
        List<Map<String, Object>> rows = getList("/api/v1/instant-payments/search?status=COMPLETED");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("endToEndId")).isEqualTo("E2E-INST-0001");
    }

    @Test
    void suspendedSearch_appliesReviewStatusFilter() {
        assertThat(getList("/api/v1/suspended-payments/search?reviewStatus=PENDING")).hasSize(2);
    }

    @Test
    void inboundSearch_appliesStatusFilter() {
        assertThat(getList("/api/v1/inbound-payments/search?status=RECEIVED")).hasSize(1);
    }

    @Test
    void outboundSearch_appliesChargesTypeFilter() {
        assertThat(getList("/api/v1/outbound-payments/search?chargesType=OUR")).hasSize(1);
    }

    @Test
    void recallsSearch_appliesRecallTypeFilter() {
        assertThat(getList("/api/v1/recalls/search?recallType=R-COR")).hasSize(1);
    }

    // ---------------- REST details ----------------

    @Test
    void instantDetails_returnsPayment() {
        String body = rest.getForObject("/api/v1/instant-payments/00000000-0000-0000-0000-000000000101", String.class);
        assertThat(JsonPath.read(body, "$.endToEndId").toString()).isEqualTo("E2E-INST-0001");
        assertThat(JsonPath.read(body, "$.status").toString()).isEqualTo("COMPLETED");
    }

    @Test
    void suspendedDetails_returnsPayment() {
        String body = rest.getForObject("/api/v1/suspended-payments/00000000-0000-0000-0000-000000000201", String.class);
        assertThat(JsonPath.read(body, "$.holdReason").toString()).isEqualTo("SANCTIONS_SCREENING");
    }

    @Test
    void inboundDetails_returnsPayment() {
        String body = rest.getForObject("/api/v1/inbound-payments/00000000-0000-0000-0000-000000000301", String.class);
        assertThat(JsonPath.read(body, "$.creditTransferRef").toString()).isEqualTo("CTR-2026-77801");
    }

    @Test
    void outboundDetails_returnsPayment() {
        String body = rest.getForObject("/api/v1/outbound-payments/00000000-0000-0000-0000-000000000401", String.class);
        assertThat(JsonPath.read(body, "$.instruction_id").toString()).isEqualTo("OUT-2026-9001");
    }

    @Test
    void recallDetails_returnsPayment() {
        String body = rest.getForObject("/api/v1/recalls/00000000-0000-0000-0000-000000000501", String.class);
        assertThat(JsonPath.read(body, "$.recallType").toString()).isEqualTo("R-COR");
    }

    @Test
    void details_unknownId_returns404() {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/v1/instant-payments/11111111-1111-1111-1111-111111111111", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void recallsSearch_unknownType_returns400() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/recalls/search?recallType=NOPE", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void instantSearch_invalidIban_returns400WithMessage() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/instant-payments/search?debtorIban=DE89", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.read(response.getBody(), "$.error").toString()).isEqualTo("Invalid debtor IBAN");
    }

    // Repository IllegalArgumentExceptions are translated by @Repository into InvalidDataAccessApiUsageException,
    // so they reach the generic 500 handler instead of the 400 one.
    @ParameterizedTest(name = "[{index}] {0} -> 500")
    @ValueSource(strings = {
            "/api/v1/inbound-payments/search?valueDateFrom=2026/10/01",
            "/api/v1/outbound-payments/search?executionTo=tomorrow",
            "/api/v1/recalls/search?originalPaymentId=not-a-uuid"
    })
    void search_malformedDateOrIdFilter_returns500(String path) {
        assertGenericServerError(rest.getForEntity(path, String.class));
    }

    @ParameterizedTest(name = "[{index}] {0}/not-a-uuid -> 500")
    @ValueSource(strings = {"/api/v1/instant-payments", "/api/v1/suspended-payments", "/api/v1/inbound-payments",
            "/api/v1/recalls"})
    void details_malformedId_returns500(String resource) {
        assertGenericServerError(rest.getForEntity(resource + "/not-a-uuid", String.class));
    }

    @Test
    void outboundDetails_malformedId_returns404() {
        assertThat(rest.getForEntity("/api/v1/outbound-payments/not-a-uuid", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void outboundFee_searchAndDetailsApplyDifferentFeeTables() {
        List<Map<String, Object>> rows = getList("/api/v1/outbound-payments/search?debtorIban=DE89370400440532013000");
        Map<String, Object> sha = rows.stream().filter(r -> "OUT-2026-9001".equals(r.get("instructionId"))).findFirst().orElseThrow();
        Map<String, Object> our = rows.stream().filter(r -> "OUT-2026-9002".equals(r.get("instructionId"))).findFirst().orElseThrow();
        assertThat(sha.get("estimatedFee").toString()).isEqualTo("0.3");
        assertThat(our.get("estimatedFee").toString()).isEqualTo("156.01");

        String shaDetails = rest.getForObject("/api/v1/outbound-payments/00000000-0000-0000-0000-000000000401", String.class);
        String ourDetails = rest.getForObject("/api/v1/outbound-payments/00000000-0000-0000-0000-000000000402", String.class);
        assertThat(JsonPath.read(shaDetails, "$.estimatedFee").toString()).isEqualTo("0.5");
        assertThat(JsonPath.read(ourDetails, "$.estimatedFee").toString()).isEqualTo("312.02");
    }

    @Test
    void outboundDetails_exposesSnakeCaseContractWithAuditTimestamps() {
        Map<String, Object> body = JsonPath.parse(rest.getForObject(
                "/api/v1/outbound-payments/00000000-0000-0000-0000-000000000401", String.class)).read("$", Map.class);
        assertThat(body).containsKeys("id", "instruction_id", "amount", "currency", "debtor_iban", "debtor_name",
                "destination_bic", "requested_execution_date", "charges_type", "status", "created_at", "updated_at",
                "estimatedFee");
        assertThat(body).doesNotContainKeys("instructionId", "debtorIban");
    }

    @Test
    void inbound_searchAndDetails_computeAgeInDaysFromValueDate() {
        long expectedAge = ChronoUnit.DAYS.between(LocalDate.parse("2026-10-15"), LocalDate.now());

        List<Map<String, Object>> rows = getList("/api/v1/inbound-payments/search?status=SETTLED");
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("ageInDays")).longValue()).isEqualTo(expectedAge);

        String details = rest.getForObject("/api/v1/inbound-payments/00000000-0000-0000-0000-000000000301", String.class);
        assertThat(((Number) JsonPath.read(details, "$.ageInDays")).longValue()).isEqualTo(expectedAge);
    }

    // ---------------- Kafka ingestion ----------------

    @Test
    void kafkaIngestion_persistsAllFivePaymentTypes() throws Exception {
        send("pay.instant", """
                {"id":"00000000-0000-0000-0000-000000000901","endToEndId":"E2E-INST-0099","uetr":"uetr-9",
                 "amount":42.00,"currency":"EUR","debtorIban":"DE89","debtorName":"ACME",
                 "creditorIban":"NL91","creditorName":"Jansen","executionDateTime":"2026-10-17 10:00:00",
                 "status":"COMPLETED","scheme":"SCT INST"}
                """);
        send("pay.suspended", """
                {"id":"00000000-0000-0000-0000-000000000902","amount":5000.00,"currency":"EUR",
                 "debtorIban":"DE89","debtorName":"ACME","creditorIban":"FR76","creditorName":"Dupont",
                 "holdReason":"SANCTIONS_SCREENING","complianceCaseRef":"CASE-2026-0099",
                 "reviewStatus":"PENDING","heldAt":"2026-10-17 11:00:00","releasedAt":null}
                """);
        send("pay.inbound", """
                {"id":"00000000-0000-0000-0000-000000000903","creditTransferRef":"CTR-2026-99901",
                 "amount":1234.56,"currency":"EUR","creditorIban":"DE89","creditorName":"ACME",
                 "originBic":"BNPAFRPP","valueDate":"2026-10-17","settlementDate":"2026-10-17","status":"SETTLED"}
                """);
        send("pay.outbound", """
                {"id":"00000000-0000-0000-0000-000000000904","instructionId":"OUT-2026-9901","amount":300.00,
                 "currency":"EUR","debtorIban":"DE89","debtorName":"ACME","destinationBic":"ABNANL2A",
                 "requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"SENT"}
                """);
        send("pay.recall", """
                {"id":"00000000-0000-0000-0000-000000000905","recallReference":"RCL-2026-9901",
                 "recallType":"R-COR","originalPaymentId":"00000000-0000-0000-0000-000000000904",
                 "originalPaymentType":"OUTBOUND","reasonCode":"AC04","amount":300.00,"currency":"EUR",
                 "initiatedAt":"2026-10-17 12:00:00","deadline":"2026-10-19","status":"INITIATED"}
                """);

        awaitRow("instant_payments", "00000000-0000-0000-0000-000000000901");
        awaitRow("suspended_payments", "00000000-0000-0000-0000-000000000902");
        awaitRow("inbound_payments", "00000000-0000-0000-0000-000000000903");
        awaitRow("outbound_payments", "00000000-0000-0000-0000-000000000904");
        awaitRow("recalls", "00000000-0000-0000-0000-000000000905");

        assertThat(jdbc.queryForObject("SELECT end_to_end_id FROM instant_payments WHERE id = ?::uuid",
                String.class, "00000000-0000-0000-0000-000000000901")).isEqualTo("E2E-INST-0099");
        assertThat(jdbc.queryForObject("SELECT compliance_case_ref FROM suspended_payments WHERE id = ?::uuid",
                String.class, "00000000-0000-0000-0000-000000000902")).isEqualTo("CASE-2026-0099");
    }

    @Test
    void kafkaIngestion_isVisibleThroughSearchApi() throws Exception {
        send("pay.instant", """
                {"id":"00000000-0000-0000-0000-000000000906","endToEndId":"E2E-INST-9906","uetr":"uetr-6",
                 "amount":7.77,"currency":"EUR","debtorIban":"DE89","debtorName":"ACME",
                 "creditorIban":"FR76","creditorName":"Dupont","executionDateTime":"2026-10-17 13:00:00",
                 "status":"PENDING","scheme":"SCT INST"}
                """);

        await().atMost(Duration.ofSeconds(30)).until(() -> countById("instant_payments",
                "00000000-0000-0000-0000-000000000906") == 1);

        List<Map<String, Object>> rows = getList("/api/v1/instant-payments/search?endToEndId=E2E-INST-9906");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("status")).isEqualTo("PENDING");
    }

    @Test
    void kafkaIngestion_upsertsBySameId() throws Exception {
        send("pay.outbound", """
                {"id":"00000000-0000-0000-0000-000000000907","instructionId":"OUT-2026-9907","amount":10.00,
                 "currency":"EUR","debtorIban":"DE89","debtorName":"ACME","destinationBic":"ABNANL2A",
                 "requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"SENT"}
                """);
        awaitRow("outbound_payments", "00000000-0000-0000-0000-000000000907");

        send("pay.outbound", """
                {"id":"00000000-0000-0000-0000-000000000907","instructionId":"OUT-2026-9907","amount":10.00,
                 "currency":"EUR","debtorIban":"DE89","debtorName":"ACME","destinationBic":"ABNANL2A",
                 "requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"ACCEPTED"}
                """);

        await().atMost(Duration.ofSeconds(30)).until(() -> "ACCEPTED".equals(jdbc.queryForObject(
                "SELECT status FROM outbound_payments WHERE id = ?::uuid", String.class,
                "00000000-0000-0000-0000-000000000907")));
    }

    @Test
    void kafkaIngestion_redelivery_resetsCreatedAt() throws Exception {
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000908","instructionId":"OUT-2026-9908","amount":10.00,
                 "currency":"EUR","debtorIban":"DE89","debtorName":"ACME","destinationBic":"ABNANL2A",
                 "requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"%s"}
                """;
        send("pay.outbound", payload.formatted("SENT"));
        awaitRow("outbound_payments", "00000000-0000-0000-0000-000000000908");
        Timestamp firstCreatedAt = createdAt("00000000-0000-0000-0000-000000000908");

        send("pay.outbound", payload.formatted("ACCEPTED"));
        await().atMost(Duration.ofSeconds(30)).until(() -> "ACCEPTED".equals(jdbc.queryForObject(
                "SELECT status FROM outbound_payments WHERE id = ?::uuid", String.class,
                "00000000-0000-0000-0000-000000000908")));

        assertThat(createdAt("00000000-0000-0000-0000-000000000908")).isAfter(firstCreatedAt);
    }

    @Test
    void kafkaIngestion_recallReferencingUnknownPayment_isPersisted() throws Exception {
        send("pay.recall", """
                {"id":"00000000-0000-0000-0000-000000000909","recallReference":"RCL-2026-9909",
                 "recallType":"R-REJ","originalPaymentId":"11111111-1111-1111-1111-111111111111",
                 "originalPaymentType":"INBOUND","reasonCode":"AM04","initiatedAt":"2026-10-17T12:00:00Z",
                 "deadline":"2026-10-19","status":"INITIATED"}
                """);

        awaitRow("recalls", "00000000-0000-0000-0000-000000000909");
        assertThat(jdbc.queryForObject("SELECT original_payment_id::text FROM recalls WHERE id = ?::uuid",
                String.class, "00000000-0000-0000-0000-000000000909")).isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void kafkaIngestion_malformedMessage_isSkippedAndNextMessageIsConsumed() throws Exception {
        send("pay.suspended", "{not json");
        send("pay.suspended", """
                {"id":"00000000-0000-0000-0000-000000000910","amount":99.00,"currency":"EUR",
                 "debtorIban":"DE89","holdReason":"MANUAL_REVIEW","complianceCaseRef":"CASE-2026-0910",
                 "reviewStatus":"PENDING","heldAt":"2026-10-17T11:00:00"}
                """);

        awaitRow("suspended_payments", "00000000-0000-0000-0000-000000000910");
    }

    // ---------------- helpers ----------------

    private void assertGenericServerError(ResponseEntity<String> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(JsonPath.read(response.getBody(), "$.error").toString())
                .isEqualTo("Something went wrong, contact support with the trace id");
    }

    private Timestamp createdAt(String id) {
        return jdbc.queryForObject("SELECT created_at FROM outbound_payments WHERE id = ?::uuid", Timestamp.class, id);
    }

    private void send(String topic, String payload) throws Exception {
        kafka.send(topic, payload).get(10, TimeUnit.SECONDS);
    }

    private void awaitRow(String table, String id) {
        await().atMost(Duration.ofSeconds(30)).until(() -> countById(table, id) == 1);
    }

    private int countById(String table, String id) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE id = ?::uuid", Integer.class, id);
        return count == null ? 0 : count;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(String path) {
        ResponseEntity<String> response = rest.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody()).read("$", List.class);
    }
}
