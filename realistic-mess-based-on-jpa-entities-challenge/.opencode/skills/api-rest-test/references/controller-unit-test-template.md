# Controller unit test template

Reference structure for a controller unit test. Adapt class names, endpoint paths, HTTP methods, DTOs, fixtures, and status codes to the actual controller; do not copy `Order*` names.

```java
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private InvoiceMapper invoiceMapper;

    @Value("classpath:/json/create-order-request.json")
    private Resource createOrderRequest;

    @Value("classpath:/json/create-order-response.json")
    private Resource createOrderResponse;

    @Test
    void shouldCreateOrder() throws Exception {
        // GIVEN
        // Prepare the expected service result and mapper result.
        // Configure the required mock behavior.

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
        // Verify the expected service and mapper interactions.
    }
}
```

`@MockitoBean` (`org.springframework.test.context.bean.override.mockito`) requires Spring Boot 3.4+; on older versions use the annotation the project already uses.
