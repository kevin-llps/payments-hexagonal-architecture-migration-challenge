package fr.kevin.llps.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import fr.kevin.llps.payments.adapter.in.kafka.inbound.KafkaInboundPaymentListener;
import fr.kevin.llps.payments.adapter.in.kafka.instant.KafkaInstantPaymentListener;
import fr.kevin.llps.payments.adapter.in.kafka.recall.KafkaRecallListener;
import fr.kevin.llps.payments.adapter.in.kafka.suspended.KafkaSuspendedPaymentListener;

import fr.kevin.llps.payments.kafka.OutboundPaymentMessageConsumer;

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
    @Autowired
    private KafkaInstantPaymentListener instantListener;
    @Autowired
    private KafkaSuspendedPaymentListener suspendedListener;
    @Autowired
    private KafkaInboundPaymentListener inboundListener;
    @Autowired
    private OutboundPaymentMessageConsumer outboundConsumer;
    @Autowired
    private KafkaRecallListener recallListener;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    // ======================= Phase 2 characterization and regression tests =======================

    private static final String NegativeLimitError = "{\"error\":\"max-results cannot be negative\"}";

    // ---------------- REST error contract (BR-SHR-03, BR-API-04..08, BR-INB-04, BR-OUT-04, BR-RCL-03, C-02) ----------------

    @ParameterizedTest(name = "[{index}] {0}?limit=-1 -> 400")
    @ValueSource(strings = {
            "/api/v1/instant-payments/search",
            "/api/v1/suspended-payments/search",
            "/api/v1/inbound-payments/search",
            "/api/v1/outbound-payments/search"
    })
    void should_return_400_when_limit_is_negative(String path) {
        // GIVEN
        String url = path + "?limit=-1";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(NegativeLimitError);
    }

    @ParameterizedTest(name = "[{index}] {0}?limit=0 -> []")
    @ValueSource(strings = {
            "/api/v1/instant-payments/search",
            "/api/v1/suspended-payments/search",
            "/api/v1/inbound-payments/search",
            "/api/v1/outbound-payments/search"
    })
    void should_return_empty_list_when_limit_is_zero(String path) {
        // GIVEN
        String url = path + "?limit=0";

        // WHEN
        List<Map<String, Object>> rows = getList(url);

        // THEN
        assertThat(rows).isEmpty();
    }

    @Test
    void should_ignore_negative_limit_for_recalls() {
        // GIVEN
        String url = "/api/v1/recalls/search?limit=-1";

        // WHEN
        List<Map<String, Object>> rows = getList(url);

        // THEN
        assertThat(rows).hasSize(3);
    }

    @ParameterizedTest(name = "[{index}] {0}/not-a-uuid -> 400")
    @ValueSource(strings = {
            "/api/v1/instant-payments",
            "/api/v1/suspended-payments",
            "/api/v1/inbound-payments",
            "/api/v1/recalls"
    })
    void should_return_400_with_invalid_uuid_message_when_details_id_is_not_a_uuid(String basePath) {
        // GIVEN
        String url = basePath + "/not-a-uuid";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("{\"error\":\"Invalid UUID string: not-a-uuid\"}");
    }

    @Test
    void should_return_404_with_empty_body_when_outbound_details_id_is_not_a_uuid() {
        // GIVEN
        String url = "/api/v1/outbound-payments/not-a-uuid";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @ParameterizedTest(name = "[{index}] {0}/unknown -> 404")
    @ValueSource(strings = {
            "/api/v1/instant-payments",
            "/api/v1/suspended-payments",
            "/api/v1/inbound-payments",
            "/api/v1/outbound-payments",
            "/api/v1/recalls"
    })
    void should_return_404_with_empty_body_when_details_id_is_unknown(String basePath) {
        // GIVEN
        String url = basePath + "/11111111-1111-1111-1111-111111111111";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @ParameterizedTest(name = "[{index}] {0}/ {1} -> 200")
    @CsvSource({
            "/api/v1/instant-payments, 00000000-0000-0000-0000-000000000101",
            "/api/v1/suspended-payments, 00000000-0000-0000-0000-000000000201",
            "/api/v1/inbound-payments, 00000000-0000-0000-0000-000000000301",
            "/api/v1/recalls, 00000000-0000-0000-0000-000000000501"
    })
    void should_trim_details_id_before_parsing_it(String basePath, String id) {
        // GIVEN
        String url = basePath + "/ " + id + " ";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.read(response.getBody(), "$.id").toString()).isEqualTo(id);
    }

    @Test
    void should_return_404_when_outbound_details_id_is_padded_with_spaces() {
        // GIVEN
        String url = "/api/v1/outbound-payments/ 00000000-0000-0000-0000-000000000401";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest(name = "[{index}] {0} -> 400 Bad request")
    @ValueSource(strings = {
            "/api/v1/inbound-payments/search?valueDateFrom=2026-13-45",
            "/api/v1/inbound-payments/search?valueDateTo=garbage",
            "/api/v1/outbound-payments/search?executionFrom=garbage",
            "/api/v1/outbound-payments/search?executionTo=2026-10-16T00:00:00"
    })
    void should_return_400_with_default_message_when_a_date_filter_is_not_a_strict_iso_date(String url) {
        // GIVEN
        String malformedFilterUrl = url;

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(malformedFilterUrl, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("{\"error\":\"Bad request\"}");
    }

    @ParameterizedTest(name = "{0}: {1} gives 400 with the number-format message")
    @CsvSource(delimiter = '|', value = {
            "inbound non-digit in the month | /api/v1/inbound-payments/search?valueDateFrom=2026-1x-05 | {\"error\":\"Error at index 1 in: \\\"1x\\\"\"}",
            "outbound non-digit in the year | /api/v1/outbound-payments/search?executionTo=20x6-10-16  | {\"error\":\"Error at index 2 in: \\\"20x6\\\"\"}"
    })
    void should_return_400_with_number_format_message_when_a_date_filter_field_is_not_numeric(
            String testCase, String url, String expectedBody) {
        // GIVEN
        String nonNumericFilterUrl = url;

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(nonNumericFilterUrl, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(expectedBody);
    }

    @Test
    void should_return_400_with_invalid_uuid_message_when_recall_original_payment_id_is_not_a_uuid() {
        // GIVEN
        String url = "/api/v1/recalls/search?originalPaymentId=xyz";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("{\"error\":\"Invalid UUID string: xyz\"}");
    }

    @Test
    void should_return_400_with_message_when_instant_debtor_iban_is_invalid() {
        // GIVEN
        String url = "/api/v1/instant-payments/search?debtorIban=DE12";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("{\"error\":\"Invalid debtor IBAN\"}");
    }

    @Test
    void should_return_400_with_raw_input_when_recall_type_is_unknown() {
        // GIVEN
        String url = "/api/v1/recalls/search?recallType=nope";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("{\"error\":\"Unknown recall type: nope\"}");
    }

    // ---------------- Outbound fee policies on seeds (BR-OUT-06..09, C-01, C-06, OQ-04) ----------------

    @ParameterizedTest(name = "[{index}] search fee of {0} = {1}")
    @CsvSource({
            "00000000-0000-0000-0000-000000000401, 0.30",
            "00000000-0000-0000-0000-000000000402, 156.01",
            "00000000-0000-0000-0000-000000000403, 0"
    })
    void should_compute_search_fee_with_service_policy_on_seeded_outbound_payments(String id, String expectedFee)
            throws Exception {
        // GIVEN
        String url = "/api/v1/outbound-payments/search";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        JsonNode row = findById(objectMapper.readTree(response.getBody()), id);
        assertThat(row.get("estimatedFee").decimalValue()).isEqualByComparingTo(expectedFee);
    }

    @ParameterizedTest(name = "[{index}] details fee of {0} = {1}")
    @CsvSource({
            "00000000-0000-0000-0000-000000000401, 0.50",
            "00000000-0000-0000-0000-000000000402, 312.02",
            "00000000-0000-0000-0000-000000000403, 0"
    })
    void should_compute_details_fee_with_controller_policy_on_seeded_outbound_payments(String id, String expectedFee)
            throws Exception {
        // GIVEN
        String url = "/api/v1/outbound-payments/" + id;

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("estimatedFee").decimalValue()).isEqualByComparingTo(expectedFee);
    }

    @Test
    void should_render_outbound_details_as_snake_case_map_with_jackson_date_formats() throws Exception {
        // GIVEN
        String url = "/api/v1/outbound-payments/00000000-0000-0000-0000-000000000401";
        DateTimeFormatter jacksonUtc = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx").withZone(ZoneOffset.UTC);

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.properties()).extracting(Map.Entry::getKey).containsExactlyInAnyOrder(
                "id", "instruction_id", "amount", "currency", "debtor_iban", "debtor_name", "destination_bic",
                "requested_execution_date", "charges_type", "status", "created_at", "updated_at", "estimatedFee");
        assertThat(response.getBody()).contains("\"amount\":780.00").contains("\"estimatedFee\":0.50");
        assertThat(body.get("requested_execution_date").asText()).isEqualTo("2026-10-16");
        assertThat(body.get("created_at").asText())
                .isEqualTo(jacksonUtc.format(Timestamp.valueOf("2026-10-16 08:00:00").toInstant()));
        assertThat(body.get("updated_at").asText())
                .isEqualTo(jacksonUtc.format(Timestamp.valueOf("2026-10-16 08:01:00").toInstant()));
        assertThat(body.get("charges_type").asText()).isEqualTo("SHA");
    }

    @Test
    void should_render_outbound_search_rows_in_camel_case_without_audit_timestamps() throws Exception {
        // GIVEN
        String url = "/api/v1/outbound-payments/search?chargesType=sha";

        // WHEN
        ResponseEntity<String> response = rest.getForEntity(url, String.class);

        // THEN
        JsonNode row = objectMapper.readTree(response.getBody()).get(0);
        assertThat(row.properties()).extracting(Map.Entry::getKey).containsExactlyInAnyOrder(
                "id", "instructionId", "amount", "currency", "debtorIban", "debtorName", "destinationBic",
                "requestedExecutionDate", "chargesType", "status", "estimatedFee");
        assertThat(row.get("requestedExecutionDate").asText()).isEqualTo("2026-10-16");
    }

    // ---------------- Instant search semantics (BR-INS-04, BR-INS-08, BR-INS-09, C-05) ----------------

    @ParameterizedTest(name = "[{index}] debtorIban={0} -> {1} rows")
    @CsvSource({
            "DE89370400440532013000, 2",
            "de89370400440532013000, 2",
            "DE89 3704 0044 0532 0130 00, 0"
    })
    void should_search_instant_debtor_iban_as_case_insensitive_contains_without_removing_inner_spaces(String iban,
            int expectedCount) {
        // GIVEN
        String url = "/api/v1/instant-payments/search?debtorIban=" + iban;

        // WHEN
        List<Map<String, Object>> rows = getList(url);

        // THEN
        assertThat(rows).hasSize(expectedCount);
    }

    @Test
    void should_return_most_recent_instant_payment_first_within_limit() {
        // GIVEN
        String url = "/api/v1/instant-payments/search?limit=1";

        // WHEN
        List<Map<String, Object>> rows = getList(url);

        // THEN
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("endToEndId")).isEqualTo("E2E-INST-0003");
            assertThat(row.get("executionDatetime")).isEqualTo("2026-10-16T14:05:00");
        });
    }

    @ParameterizedTest(name = "[{index}] {0} details shape equals search shape")
    @CsvSource({
            "/api/v1/instant-payments, 00000000-0000-0000-0000-000000000101",
            "/api/v1/suspended-payments, 00000000-0000-0000-0000-000000000201",
            "/api/v1/recalls, 00000000-0000-0000-0000-000000000501"
    })
    void should_return_same_payload_in_details_and_search(String basePath, String id) {
        // GIVEN
        Map<String, Object> fromSearch = getList(basePath + "/search").stream()
                .filter(row -> id.equals(row.get("id"))).findFirst().orElseThrow();

        // WHEN
        String body = rest.getForObject(basePath + "/" + id, String.class);

        // THEN
        Map<String, Object> fromDetails = JsonPath.parse(body).read("$", Map.class);
        assertThat(fromDetails).isEqualTo(fromSearch);
    }

    // ---------------- Suspended search semantics (BR-SUS-01, BR-SUS-04, BR-SUS-06, BR-API-09, C-04, C-05) ----------------

    @ParameterizedTest(name = "[{index}] debtorIban={0} -> {1} rows")
    @CsvSource({
            "DE89370400440532013000, 1",
            "de89370400440532013000, 0",
            "DE89, 0"
    })
    void should_search_suspended_debtor_iban_exactly_without_validation(String iban, int expectedCount) {
        // GIVEN
        String url = "/api/v1/suspended-payments/search?debtorIban=" + iban;

        // WHEN
        List<Map<String, Object>> rows = getList(url);

        // THEN
        assertThat(rows).hasSize(expectedCount);
    }

    @ParameterizedTest(name = "[{index}] {0} -> []")
    @ValueSource(strings = {"heldFrom=2026-10-01", "heldTo=garbage"})
    void should_return_empty_suspended_result_when_held_bound_is_not_iso_date_time(String filter) {
        // GIVEN
        String url = "/api/v1/suspended-payments/search?" + filter;

        // WHEN
        List<Map<String, Object>> rows = getList(url);

        // THEN
        assertThat(rows).isEmpty();
    }

    @Test
    void should_serialize_unreleased_suspended_payment_with_explicit_null_released_at() {
        // GIVEN
        String url = "/api/v1/suspended-payments/00000000-0000-0000-0000-000000000201";

        // WHEN
        String body = rest.getForObject(url, String.class);

        // THEN
        assertThat(body).contains("\"releasedAt\":null").contains("\"heldAt\":\"2026-10-14T08:00:00\"");
    }

    // ---------------- Inbound age in days (BR-INB-07, R-03) ----------------

    @Test
    void should_compute_inbound_details_age_in_days_from_value_date_to_today() {
        // GIVEN
        long expectedAge = ChronoUnit.DAYS.between(LocalDate.of(2026, 10, 15), LocalDate.now());

        // WHEN
        String body = rest.getForObject("/api/v1/inbound-payments/00000000-0000-0000-0000-000000000301", String.class);

        // THEN
        assertThat(((Number) JsonPath.read(body, "$.ageInDays")).longValue()).isEqualTo(expectedAge);
    }

    @Test
    void should_compute_inbound_search_age_in_days_for_every_row() {
        // GIVEN
        long expectedAge = ChronoUnit.DAYS.between(LocalDate.of(2026, 10, 16), LocalDate.now());

        // WHEN
        List<Map<String, Object>> rows = getList("/api/v1/inbound-payments/search?status=received");

        // THEN
        assertThat(rows).singleElement()
                .satisfies(row -> assertThat(((Number) row.get("ageInDays")).longValue()).isEqualTo(expectedAge));
    }

    // ---------------- Ingestion semantics (BR-ING-03..07, BR-RCL-07) ----------------

    @ParameterizedTest(name = "[{index}] {0} commit failure propagates")
    @CsvSource({
            "instant, instant_payments, 00000000-0000-0000-0000-000000000941",
            "suspended, suspended_payments, 00000000-0000-0000-0000-000000000942",
            "inbound, inbound_payments, 00000000-0000-0000-0000-000000000943",
            "outbound, outbound_payments, 00000000-0000-0000-0000-000000000944",
            "recall, recalls, 00000000-0000-0000-0000-000000000945"
    })
    void should_propagate_commit_time_constraint_violation_out_of_the_listener(String type, String table, String id) {
        // GIVEN
        String payload = "{\"id\":\"" + id + "\",\"amount\":1.00,\"currency\":\"EURO\","
                + "\"originalPaymentId\":\"00000000-0000-0000-0000-000000000401\"}";
        Consumer<String> listener = listenerFor(type);

        // WHEN / THEN
        assertThatThrownBy(() -> listener.accept(payload))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("value too long");
        assertThat(countById(table, id)).isZero();
    }

    @Test
    void should_overwrite_created_at_when_the_same_id_is_ingested_again() throws Exception {
        // GIVEN
        String id = "00000000-0000-0000-0000-000000000946";
        send("pay.outbound", outboundEvent(id, "SENT"));
        awaitRow("outbound_payments", id);
        Timestamp firstCreatedAt = createdAtOf(id);

        // WHEN
        send("pay.outbound", outboundEvent(id, "ACCEPTED"));

        // THEN
        await().atMost(Duration.ofSeconds(30)).until(() -> "ACCEPTED".equals(statusOf(id)));
        assertThat(createdAtOf(id)).isAfter(firstCreatedAt);
    }

    @Test
    void should_fully_replace_columns_missing_from_the_later_event() throws Exception {
        // GIVEN
        String id = "00000000-0000-0000-0000-000000000947";
        send("pay.instant", "{\"id\":\"" + id + "\",\"amount\":5.00,\"currency\":\"EUR\","
                + "\"debtorName\":\"ACME\",\"status\":\"PENDING\"}");
        awaitRow("instant_payments", id);

        // WHEN
        send("pay.instant", "{\"id\":\"" + id + "\",\"amount\":5.00,\"currency\":\"EUR\","
                + "\"status\":\"COMPLETED\"}");

        // THEN
        await().atMost(Duration.ofSeconds(30)).until(() -> "COMPLETED".equals(jdbc.queryForObject(
                "SELECT status FROM instant_payments WHERE id = ?::uuid", String.class, id)));
        assertThat(jdbc.queryForObject("SELECT debtor_name FROM instant_payments WHERE id = ?::uuid", String.class, id))
                .isEmpty();
    }

    @Test
    void should_skip_malformed_message_and_keep_consuming_the_topic() throws Exception {
        // GIVEN
        String id = "00000000-0000-0000-0000-000000000948";
        send("pay.inbound", "{not valid json");

        // WHEN
        send("pay.inbound", "{\"id\":\"" + id + "\",\"amount\":10.00,\"currency\":\"EUR\","
                + "\"valueDate\":\"2026-10-17\",\"status\":\"RECEIVED\"}");

        // THEN
        awaitRow("inbound_payments", id);
        assertThat(jdbc.queryForObject("SELECT status FROM inbound_payments WHERE id = ?::uuid", String.class, id))
                .isEqualTo("RECEIVED");
    }

    @Test
    void should_persist_missing_fields_as_empty_strings_and_json_nulls_as_null_literal() throws Exception {
        // GIVEN
        String id = "00000000-0000-0000-0000-000000000949";

        // WHEN
        send("pay.suspended", "{\"id\":\"" + id + "\",\"currency\":\"EUR\",\"creditorName\":null}");

        // THEN
        awaitRow("suspended_payments", id);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT amount, debtor_name, creditor_name, held_at FROM suspended_payments WHERE id = ?::uuid", id);
        assertThat((BigDecimal) row.get("amount")).isEqualByComparingTo("0");
        assertThat(row.get("debtor_name")).isEqualTo("");
        assertThat(row.get("creditor_name")).isEqualTo("null");
        assertThat(row.get("held_at")).isNull();
    }

    @Test
    void should_persist_recall_referencing_an_unknown_original_payment() throws Exception {
        // GIVEN
        String id = "00000000-0000-0000-0000-000000000950";

        // WHEN
        send("pay.recall", "{\"id\":\"" + id + "\",\"recallType\":\"R-XXX\","
                + "\"originalPaymentId\":\"99999999-9999-9999-9999-999999999999\","
                + "\"originalPaymentType\":\"CHEQUE\",\"status\":\"INITIATED\"}");

        // THEN
        awaitRow("recalls", id);
        assertThat(jdbc.queryForObject("SELECT recall_type FROM recalls WHERE id = ?::uuid", String.class, id))
                .isEqualTo("R-XXX");
    }

    // ---------------- Data and environment (BR-DAT-01, BR-DAT-02) ----------------

    @Test
    void should_expose_exactly_the_five_payment_tables() {
        // GIVEN
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";

        // WHEN
        List<String> tables = jdbc.queryForList(sql, String.class);

        // THEN
        assertThat(tables).containsExactlyInAnyOrder(
                "instant_payments", "suspended_payments", "inbound_payments", "outbound_payments", "recalls");
    }

    @ParameterizedTest(name = "[{index}] {0} has 3 seeded rows")
    @ValueSource(strings = {"instant_payments", "suspended_payments", "inbound_payments", "outbound_payments", "recalls"})
    void should_keep_three_seeded_rows_per_table_outside_the_reserved_test_range(String table) {
        // GIVEN
        String sql = "SELECT count(*) FROM " + table
                + " WHERE id::text NOT LIKE '00000000-0000-0000-0000-0000000009%'";

        // WHEN
        Integer count = jdbc.queryForObject(sql, Integer.class);

        // THEN
        assertThat(count).isEqualTo(3);
    }

    // ---------------- Phase 2 helpers ----------------

    private Consumer<String> listenerFor(String type) {
        return switch (type) {
            case "instant" -> instantListener::onMessage;
            case "suspended" -> suspendedListener::onMessage;
            case "inbound" -> inboundListener::onMessage;
            case "outbound" -> outboundConsumer::onMessage;
            case "recall" -> recallListener::onMessage;
            default -> throw new IllegalArgumentException(type);
        };
    }

    private static String outboundEvent(String id, String status) {
        return "{\"id\":\"" + id + "\",\"instructionId\":\"OUT-" + id.substring(33) + "\",\"amount\":10.00,"
                + "\"currency\":\"EUR\",\"chargesType\":\"SHA\",\"status\":\"" + status + "\"}";
    }

    private Timestamp createdAtOf(String id) {
        return jdbc.queryForObject("SELECT created_at FROM outbound_payments WHERE id = ?::uuid", Timestamp.class, id);
    }

    private String statusOf(String id) {
        return jdbc.queryForObject("SELECT status FROM outbound_payments WHERE id = ?::uuid", String.class, id);
    }

    private static JsonNode findById(JsonNode rows, String id) {
        for (JsonNode row : rows) {
            if (id.equals(row.get("id").asText())) {
                return row;
            }
        }
        throw new AssertionError("No row with id " + id);
    }

    // ---------------- helpers ----------------

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
