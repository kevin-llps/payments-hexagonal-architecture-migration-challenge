# Java coding conventions (new and migrated code)

These conventions apply to **new and migrated code only**. Legacy code keeps its intentional smells (see `../README.md`, "Known smells (intentional — do not fix, migrate)", and `../AGENTS.md`) until it is migrated; never use these rules to justify a drive-by cleanup.

When a skill under `.claude/skills/` defines a stricter rule for its own output (e.g. `unit-test` for test code), the skill wins for that output.

## Language level

- Target Java 21.
- Use `record` for immutable data carriers (value objects, DTOs, commands, events).
- Use `sealed` interfaces when a type has a closed, known set of variants.
- Use pattern matching for `instanceof` and `switch` instead of casts.
- Use text blocks for multi-line strings (JSON fixtures, JPQL).
- Use `var` for local variables only when the type is obvious from the right-hand side.

## Naming

- Classes, interfaces, records, and enums use `PascalCase`.
- Methods, fields, parameters, and local variables use `camelCase`.
- Production constants (`static final`) use `UPPER_SNAKE_CASE`.
- Packages are lowercase, without underscores.
- Ports are named after the capability they expose, without technology: `SearchInstantPayments`, `SaveOutboundPayment`.
- Adapters are prefixed by their technology: `JpaInstantPaymentRepositoryAdapter`, `KafkaInboundPaymentListener`, `RestOutboundPaymentController`.
- Test classes use the tested class name followed by `Test` (unit) or `IntegrationTest` (Spring context / Docker).

## Immutability

- Domain objects are immutable: `record` or `private final` fields.
- No public mutable fields.
- No setters in the domain; state changes return a new instance or go through intention-revealing methods.
- Collections exposed by an object are unmodifiable copies (`List.copyOf`, `Set.copyOf`, `Map.copyOf`).

## Null handling

- Public domain methods never return `null`.
- Return `Optional<T>` for an absent single result; never use `Optional` as a field or parameter type.
- Return an empty collection for an absent list, never `null`.
- Validate invariants in constructors or record compact constructors (`Objects.requireNonNull`, explicit checks).

## Dependency injection

- Use constructor injection only.
- Declare injected dependencies `private final`.
- No field injection (`@Autowired` on fields).
- `@PersistenceContext` / `EntityManager` is only allowed in infrastructure adapters.

## Framework isolation

- The `domain` module imports no Spring, Jakarta Persistence, Hibernate, Jackson, Kafka, or JDBC types.
- The `domain` module uses no framework annotations.
- Framework configuration and bean wiring live in `bootstrap`.

## Types

- Use `BigDecimal` for monetary amounts and fees; never `double` or `float`.
- Set `BigDecimal` scale and `RoundingMode` explicitly when dividing or rounding.
- Use `java.time` (`Instant`, `LocalDate`, `OffsetDateTime`) instead of `java.util.Date`, `java.sql.Timestamp`, or `SimpleDateFormat`; conversions to legacy types happen only in adapters.
- Use `java.util.UUID` for identifiers.
- Use enums for closed sets of values (statuses, charge bearers) instead of raw strings.

## Exceptions

- Throw domain-specific unchecked exceptions for business rule violations.
- Never swallow an exception: rethrow, translate, or log and handle it explicitly.
- No `catch (Exception e)` except at adapter boundaries (listeners, controllers advice), where it must log and map explicitly.
- Do not use exceptions for normal control flow.

## Methods and classes

- One responsibility per class (see the `single-responsibility-principle` skill).
- Keep methods short and single-purpose.
- Prefer early returns over nested conditionals.
- No static utility classes holding business logic; business logic belongs to the domain.
- No magic numbers or strings: name them as constants or enum values.

## Logging

- Use SLF4J (`org.slf4j.Logger`).
- Use parameterized messages: `log.info("Saved payment {}", id)`; no string concatenation.
- No `System.out` / `System.err` / `printStackTrace()`.
- Log exceptions by passing them as the last argument: `log.error("Failed to parse {}", topic, e)`.

## Tests

- Use JUnit 5, AssertJ, and Mockito.
- Test method names use `snake_case`: `should_return_payment_when_id_exists()`.
- Every test has `// GIVEN`, `// WHEN`, `// THEN` sections.
- Underscores in test code are allowed only in test method names, as required by the `unit-test` skill; test constants use `camelCase` or `PascalCase`, not `UPPER_SNAKE_CASE`.
- Unit tests mock collaborators and need no Docker, database, or Kafka.
- Use `@MockitoBean`, never the deprecated `@MockBean`.
- Async assertions use Awaitility, never `Thread.sleep`.

## Formatting

- Indent with 4 spaces; no tabs.
- One top-level type per file.
- No wildcard imports.
- Order imports: regular imports first, static imports last.
- Use braces for every `if`, `else`, `for`, and `while` body.
