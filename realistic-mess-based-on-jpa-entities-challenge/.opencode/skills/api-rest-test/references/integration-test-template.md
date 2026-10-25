# Integration test template

Illustrative structure for a REST API integration test through MockMvc. Use the actual repositories, entities, and application configuration; do not introduce a repository dependency if the endpoint does not use persistence. If the project's integration tests use a real HTTP client or a random-port server, follow that approach instead.

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Value("classpath:/json/create-order-request.json")
    private Resource createOrderRequest;

    @Value("classpath:/json/create-order-response.json")
    private Resource createOrderResponse;

    @Test
    void shouldCreateOrder() throws Exception {
        // GIVEN
        // Prepare the required database state.
        // Clean up or initialize test data when necessary.

        // WHEN / THEN
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderRequest.getContentAsString(
                                StandardCharsets.UTF_8)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        createOrderResponse.getContentAsString(
                                StandardCharsets.UTF_8)));

        // THEN
        // Verify that the expected entity was persisted.
        // Assert relevant database state or other observable effects.
    }
}
```

Point the context at the configured test database or a Testcontainers instance; never at production systems or production credentials.
