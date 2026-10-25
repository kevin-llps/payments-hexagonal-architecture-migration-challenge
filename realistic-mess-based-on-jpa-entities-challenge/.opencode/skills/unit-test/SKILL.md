---
name: Unit Test
description: Write focused, FIRST-compliant JUnit 5 + Mockito unit tests for one public method at a time, with snake_case test names, GIVEN / WHEN / THEN sections, mocked collaborators, and verified interactions. Use when a feature or public method needs new or updated unit tests.
---

# Unit Test

## Purpose

Write high-quality, reliable, and maintainable unit tests that validate the
behavior of a given feature or public method.

## Scope

**In scope**

- Testing a single public method at a time.
- Covering all relevant atomic acceptance criteria of that method.
- Following the project's existing testing framework, conventions, and architecture.
- Applying the FIRST principles (see below).

**Out of scope**

- Testing private methods or implementation details directly.
- Tests that need real infrastructure (database, broker, network); unit tests
  mock those collaborators instead.
- Modifying tests that cover unrelated features.
- Weakening assertions to make a test pass.

## Workflow

1. **Read** the public method under test, its collaborators, and the nearest
   existing test classes; reuse the project's
   existing Java, JUnit, AssertJ, and Mockito conventions
   (imports, assertion style, test location).
2. **List** the atomic behaviors and acceptance criteria to validate: the
   successful path, boundaries, invalid inputs, and exceptions.
3. **Write** one focused test per behavior, applying the conventions below.
4. **Run** the relevant tests and report the results, including failures or
   limitations.

## Naming conventions

**Test method names**

- Use `snake_case` for test method names.
- Names clearly describe the scenario and expected behavior.

```java
void should_return_invoice_when_order_is_valid()
void should_throw_exception_when_order_is_invalid()
void should_not_call_invoice_service_when_validation_fails()
```

**All other identifiers**

- Underscores are allowed exclusively in test method names. Never use
  underscores in class names, variable names, field names, parameters, or
  production method names.
- Use **PascalCase** for class names.
- Use **camelCase** for variables, fields, parameters, and methods.
- Follow the project's existing naming conventions for constants without
  underscores.

```java
class OrderServiceTest {

    private OrderService orderService;
    private TransactionService transactionService;
    private InvoiceService invoiceService;
}
```

## Test structure: GIVEN / WHEN / THEN

Every unit test follows the GIVEN / WHEN / THEN structure, with comments
separating the three sections:

- **GIVEN:** prepare test data, configure mock behavior, define initial
  conditions.
- **WHEN:** execute the public method under test exactly once.
- **THEN:** assert the expected outcome and verify relevant interactions with
  dependencies.

```java
@Test
void should_return_invoice_when_order_is_valid() {
    // GIVEN
    // Prepare test data and configure mocks.

    // WHEN
    // Execute the method under test.

    // THEN
    // Assert the result and verify interactions.
}
```

Each test focuses on one behavior and never combines unrelated scenarios.

## Mockito

Use Mockito to mock object dependencies so each test isolates the class under
test from its collaborators.

### Annotations

- Use `@ExtendWith(MockitoExtension.class)` to initialize Mockito for JUnit 5.
- Use `@InjectMocks` on the class under test to inject its mocked dependencies.
- Use `@Mock` for dependencies whose behavior must be controlled or whose
  interactions must be verified.
- Prefer constructor injection in production code when consistent with the
  project's architecture.
- Do not manually instantiate the class under test with mocked dependencies
  when `@InjectMocks` already handles the injection.
- Do not mock the class under test itself.
- Avoid mocking simple value objects, DTOs, or domain objects when real
  instances can be used.

### Stubbing

- Use `when(...).thenReturn(...)` to define the return values of mocked
  dependencies.
- Use `when(...).thenThrow(...)` when testing exception scenarios.
- Configure only the behavior the current test needs; avoid unnecessary
  stubbing and shared mutable test state.
- Use realistic test data and make the relationship between inputs, mock
  responses, and expected outputs explicit.

### Verifying interactions

- Use `verify(...)` to confirm the expected dependencies are called with the
  correct arguments.
- Verify invocation counts when relevant.
- Use `verify(mock, never())` when a dependency must not be called.
- Use `verifyNoMoreInteractions(...)` to detect unexpected additional
  interactions when strict interaction verification is appropriate, and
  include all relevant mocks in it when no further interactions may occur.
- Do not use `verifyNoMoreInteractions(...)` indiscriminately when additional
  interactions are not part of the behavior being tested.
- Always assert the actual outcome (e.g. `assertThat(actual).isEqualTo(expected)`)
  as well as verifying interactions, when applicable.

### Example

Follow the Mockito configuration and verification style of
[`references/order-service-test-example.md`](references/order-service-test-example.md)
(`OrderServiceTest`), adapting class and method names to the project. Use
consistent dependency names, and make sure every mocked dependency referenced
in a test is declared and injected correctly.

## FIRST principles

All unit tests must respect:

- **Fast**: tests execute quickly; avoid slow external operations.
- **Independent**: each test runs in isolation, regardless of other tests or
  execution order.
- **Repeatable**: results remain consistent across executions.
- **Self-validating**: assertions and verifications determine success
  automatically.
- **Timely**: tests are developed alongside the feature implementation.

## Coverage and constraints

- Cover every relevant atomic acceptance criterion.
- Include successful scenarios, boundary conditions, invalid inputs, and exception cases where applicable.
- Test observable behavior rather than private implementation details.
- Mock external collaborators rather than invoking real infrastructure.
- Do not modify tests covering unrelated features.
- Do not weaken assertions to make tests pass.
- Avoid unnecessary production code, mocks, stubs, and assertions.
- Never use underscores outside test method names.
- Keep tests readable, deterministic, maintainable, and focused.

## Deliverables

For each public method under test, provide:

1. The atomic behaviors and acceptance criteria being validated.
2. Unit tests with `snake_case` test method names.
3. Class names, variables, fields, parameters, and production methods without
   underscores.
4. Clearly separated GIVEN / WHEN / THEN sections.
5. Mockito-based dependency isolation with `@Mock` and `@InjectMocks` where
   appropriate.
6. Appropriate stubbing, result assertions, `verify(...)`, and
   `verifyNoMoreInteractions(...)` where justified.
7. Relevant edge-case and exception tests.
8. A summary of test execution results, including failures or limitations.

## Definition of done

All relevant behaviors are covered by focused unit tests. Tests follow the
naming conventions, the GIVEN / WHEN / THEN structure, and the FIRST
principles. Dependencies are mocked appropriately, interactions are verified
where meaningful, and all relevant tests execute successfully.

## Rules

| ID  | Rule                                                                                         | Check            |
| --- | -------------------------------------------------------------------------------------------- | ---------------- |
| R1  | The skill is discoverable and its description says when to use it                            | static           |
| R2  | Tests target a single public method at a time and cover all its atomic acceptance criteria   | static           |
| R3  | Tests follow the project's existing testing framework, conventions, and architecture         | static           |
| R4  | Test method names use snake_case and describe the scenario and expected behavior             | static           |
| R5  | Underscores are forbidden outside test method names; classes PascalCase, the rest camelCase  | static           |
| R6  | Every test is split into commented GIVEN / WHEN / THEN sections                              | static           |
| R7  | WHEN executes the method under test exactly once; each test focuses on one behavior          | static           |
| R8  | Dependencies are isolated with @ExtendWith(MockitoExtension.class), @InjectMocks and @Mock   | static           |
| R9  | The class under test is never mocked or hand-built; simple value objects are real instances  | static           |
| R10 | Mocks are stubbed with when/thenReturn or when/thenThrow, only as needed, with realistic data | static           |
| R11 | Interactions are verified with verify, invocation counts, and verify(mock, never())          | static           |
| R12 | verifyNoMoreInteractions lists all relevant mocks and is used only when justified            | static           |
| R13 | The actual outcome is always asserted in addition to verifying interactions                  | static           |
| R14 | The reference example ships in references/ and is linked from SKILL.md                       | static           |
| R15 | All tests respect the five FIRST principles                                                  | static           |
| R16 | Coverage includes success, boundary, invalid-input and exception scenarios                   | static           |
| R17 | Collaborators are mocked, unrelated tests untouched, assertions never weakened               | static           |
| R18 | Deliverables include the validated behaviors and a summary of test execution results         | static           |
| R19 | The definition of done requires all relevant tests to execute successfully                   | static           |
| R20 | The skill ships a README.md documentation file                                               | static           |
| R21 | A request to test a service method yields a compliant Mockito unit test class                | agent-evaluation |
| R22 | Requests to hit real infrastructure or weaken an assertion are declined                      | agent-evaluation |

## Acceptance criteria

- [ ] Each test targets one public method and one behavior, and together they
      cover every relevant atomic acceptance criterion.
- [ ] Test method names are `snake_case`; no other identifier contains an
      underscore.
- [ ] Every test has `// GIVEN`, `// WHEN`, `// THEN` sections and calls the
      method under test exactly once.
- [ ] Collaborators are `@Mock`s injected via `@InjectMocks` under
      `@ExtendWith(MockitoExtension.class)`; the class under test is real.
- [ ] Outcomes are asserted and interactions verified (`verify`, `never()`,
      `verifyNoMoreInteractions` where justified).
- [ ] Success, boundary, invalid-input, and exception cases are covered.
- [ ] The relevant tests run and pass, and the report lists the validated
      behaviors plus the execution results.
