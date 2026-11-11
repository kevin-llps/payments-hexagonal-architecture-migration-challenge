---
name: Parameterized Unit Test
description: Write or refactor JUnit 5 unit tests so that cases exercising the same method with different data sets become one parameterized test with explicit display names. Use when several unit tests of one public method differ only in their input values or expected results, or when adding data-driven cases to a unit test.
---

# Parameterized Unit Test

## Invocation Announcement

Before any other output or tool call, display exactly this line as the skill's first output:

`▶ Skill invoked: Parameterized Unit Test — merge data-driven unit test cases into parameterized tests`

Display it on every invocation, whether triggered by the user, an agent, or a skill. When delegating to a subagent or loading a skill, do not print its announcement on its behalf; it prints its own. The announcement never replaces or delays a mandatory approval gate.

Write high-quality, maintainable unit tests that minimize duplication by using
parameterized tests whenever multiple test cases exercise the same method with
different input data sets or expected outcomes.

## Purpose

Collapse unit tests that repeat the same structure with different data into
one parameterized test per behavior, while keeping every invocation explicit,
readable, and easy to diagnose when it fails.

## Scope

### In scope

- Focus on testing a single public method at a time.
- Identify opportunities to mutualize tests that exercise the same behavior
  with different data sets.
- Use parameterized tests to cover multiple scenarios without duplicating test
  structure.
- Follow the project's existing testing framework, conventions, and
  architecture.

### Out of scope

- Merging unrelated behaviors into one parameterized test. Tests whose setup,
  execution, or assertions are materially different (for example a success
  path that stubs and verifies a repository vs. a validation failure that
  throws and touches no collaborator) stay separate tests.
- General unit-test conventions (test naming, GIVEN / WHEN / THEN layout,
  mock setup, FIRST principles). They still apply to every parameterized test
  but are owned by the `unit-test` skill and are not repeated here.

## Workflow

1. **Pick one public method** and read every existing test that exercises it.
2. **Group by behavior** — tests with the same setup, the same call, and the
   same kind of assertions that differ only in data are one behavior. Tests
   whose setup, execution, or assertions differ materially stay separate.
3. **Choose the argument source** (`@ValueSource`, `@CsvSource`,
   `@MethodSource`) that fits the shape of the data, as described below.
4. **Write one parameterized test per behavior** — a single shared
   GIVEN / WHEN / THEN body, one data row per former case, each row with its
   expected outcome and an explicit display name.
5. **Remove the replaced test methods** only once every former case exists as
   a data row, so no scenario is lost.
6. **Run the tests** with the project's runner (here, from the module:
   `mvn test -Dtest=<TestClass>`), confirm every invocation passes, and check
   that each invocation's display name reads as scenario plus data.

## Parameterized test strategy

- When multiple tests exercise the same method using different input data sets,
  expected results, or equivalent scenarios, prefer a parameterized test.
- Mutualize the common GIVEN / WHEN / THEN structure and test logic into a
  single parameterized test.
- Use JUnit 5 `@ParameterizedTest` with the argument source that fits the
  data, where appropriate:
  - `@ValueSource` — one simple argument per invocation (strings, numbers).
  - `@CsvSource` — a few scalar columns per row, such as a description, an
    input, and an expected value.
  - `@MethodSource` — rows that need objects, `Optional`s, collections, or
    `Named` arguments, supplied by a `static Stream<Arguments>` factory.
- Use the project's existing parameterized testing capabilities; do not add a
  new testing library. This module already has JUnit 5 Jupiter (including
  `junit-jupiter-params`) and Mockito on its test classpath.
- Keep test data separate from test execution logic when this improves
  readability and maintainability (for example a `static` factory method next
  to the test, referenced by `@MethodSource`).
- Ensure every data set represents a meaningful test case with an explicit expected outcome: the expected value travels in the row, it is never
  recomputed inside the test body.
- Avoid duplicating test methods that differ only in their input values or expected results.
- Do not force unrelated behaviors into a single parameterized test. Keep
  separate tests when their setup, execution, or assertions are materially
  different; a parameterized test whose body branches on its arguments
  (`if`/`switch` on the row) is a sign the cases belong in separate tests.
- Preserve the readability and diagnostic value of individual test cases.

## Explicit display names

- Every parameterized test must provide an explicit, meaningful display name.
- The display name must identify both the **test case** and the **data set**
  being executed: set it with `@ParameterizedTest(name = "...")`, referencing
  a scenario description and the relevant input values.
- Prefer descriptive names that explain the scenario and relevant input values.
- Do not rely solely on generated default display names or numeric indexes:
  `[{index}]` alone, or `{arguments}` rendered from `toString()`, tells the
  reader nothing about the scenario.
- Use JUnit 5's `@ParameterizedTest(name = "...")` and, where appropriate,
  `Named` arguments or a dedicated test-case description (a leading
  `String testCase` column/argument).
- Avoid display names that are ambiguous, excessively verbose, or dependent on object memory addresses (such as `Customer@1b6d3586`).
- Ensure failure reports clearly identify which scenario and data set failed,
  without opening the data provider.

```java
@ParameterizedTest(name = "{0}: {1} rounds to {2}")
@CsvSource({
    "half-up on the third decimal, 10.005, 10.01",
    "already two decimals,         10.00,  10.00"
})
void should_round_amount_to_two_decimals(String testCase, BigDecimal amount, BigDecimal expected) {
    // GIVEN / WHEN / THEN
}
```

When an argument is an object whose `toString()` is not readable, wrap it in
`Named.of(...)` so `{0}` renders the label instead of the object:

```java
static Stream<Arguments> paymentStatusCases() {
    return Stream.of(
        Arguments.of(Named.of("settled payment", settledPayment()), "SETTLED"),
        Arguments.of(Named.of("rejected payment", rejectedPayment()), "REJECTED")
    );
}
```

## Reference example

One public method (`CustomerService.findCustomer`), one behavior, two data
sets. `{0}` is the test-case description, so the display name carries both the
scenario and its data:

```java
@ParameterizedTest(name = "[{index}] {0}")
@MethodSource("customerLookupCases")
void should_return_expected_customer_for_given_input(
        String testCase,
        Long customerId,
        Optional<Customer> expectedCustomer) {
    // GIVEN
    when(customerRepository.findById(customerId))
        .thenReturn(expectedCustomer);

    // WHEN
    Optional<Customer> result = customerService.findCustomer(customerId);

    // THEN
    assertEquals(expectedCustomer, result);
    verify(customerRepository).findById(customerId);
    verifyNoMoreInteractions(customerRepository);
}

static Stream<Arguments> customerLookupCases() {
    return Stream.of(
        Arguments.of(
            "existing customer with id 1 returns the customer",
            1L,
            Optional.of(new Customer(1L))
        ),
        Arguments.of(
            "unknown customer with id 2 returns empty",
            2L,
            Optional.empty()
        )
    );
}
```

Each execution displays a name such as:

- `existing customer with id 1 returns the customer`
- `unknown customer with id 2 returns empty`

The examples are illustrative; adapt method signatures and test data to the
actual project (its assertion library, entities, and repositories).

## Rules

| ID  | Rule                                                                   | Check            |
| --- | ---------------------------------------------------------------------- | ---------------- |
| R1  | Frontmatter says when to load the skill                                | static           |
| R2  | Directory name equals the kebab-case skill ID                          | static           |
| R3  | Tests target one public method at a time                               | static           |
| R4  | Same method, several data sets: parameterize                           | static           |
| R5  | Shared test skeleton is written once                                   | static           |
| R6  | JUnit 5 argument sources are chosen per data shape                     | static           |
| R7  | Project tooling and conventions are reused                             | static           |
| R8  | Data providers are separated from the test body                        | static           |
| R9  | Each row carries its own expected result                               | static           |
| R10 | No copy-pasted test methods that vary only by data                     | static           |
| R11 | Unrelated behaviors stay in their own tests                            | static           |
| R12 | Each invocation stays readable and diagnosable                         | static           |
| R13 | Explicit name for every parameterized test, covering case and data     | static           |
| R14 | No index-only or default names; `Named` or description column          | static           |
| R15 | Names avoid ambiguity, verbosity, and object identity strings          | static           |
| R16 | A failing invocation is identifiable from the report                   | static           |
| R17 | The customer lookup reference example is included                      | static           |
| R18 | General unit-test conventions are delegated, not duplicated            | static           |
| R19 | Definition of done is stated                                           | static           |
| R20 | Rules table and spec list the same IDs                                 | static           |
| R21 | README ships beside SKILL.md                                           | static           |
| R22 | Data-only duplicates are merged with explicit display names            | agent-evaluation |
| R23 | Materially different behaviors are not merged                          | agent-evaluation |
| R24 | The first output line announces the skill invocation                   | static           |

## Definition of done

- [ ] Repeated test logic is mutualized where appropriate.
- [ ] Parameterized tests cover the relevant data sets.
- [ ] Every invocation has an explicit and informative display name.
- [ ] The FIRST principles are respected.
- [ ] Mockito interactions are verified appropriately.
- [ ] The test run is green: all relevant tests execute successfully.
- [ ] The first output line is `▶ Skill invoked: Parameterized Unit Test — merge data-driven unit test cases into parameterized tests`.
