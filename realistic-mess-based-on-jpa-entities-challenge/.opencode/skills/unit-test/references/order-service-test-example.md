# Reference example: OrderServiceTest

Follow this example's Mockito configuration and verification style, adapting
class names and method calls to the actual project.

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private InvoiceService invoiceService;

    @Test
    void should_validate_order() {
        // GIVEN
        Cart cart = new Cart();
        PurchaseTransactionDetails purchaseTransactionDetails =
            new PurchaseTransactionDetails("data1", "data2", "data3");
        Invoice expected = new Invoice(123456, purchaseTransactionDetails);

        when(transactionService.validate(cart))
            .thenReturn(purchaseTransactionDetails);
        when(invoiceService.create(purchaseTransactionDetails))
            .thenReturn(expected);

        // WHEN
        Invoice actual = orderService.validateOrder(cart);

        // THEN
        assertThat(actual).isEqualTo(expected);

        verify(transactionService).validate(cart);
        verify(invoiceService).create(purchaseTransactionDetails);

        verifyNoMoreInteractions(transactionService, invoiceService);
    }
}
```

Important:

- Use consistent dependency names throughout the test.
- Ensure every mocked dependency referenced in the test is declared and
  injected correctly.
- Keep test method names in `snake_case`, while class names, variables,
  fields, parameters, and production methods use the appropriate naming
  convention without underscores.
