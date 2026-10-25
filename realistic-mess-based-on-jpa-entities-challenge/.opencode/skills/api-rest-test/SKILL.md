---
name: API REST Test
description: Generate Spring Boot REST controller unit tests (@WebMvcTest, MockMvc, @MockitoBean) and REST API integration tests (@SpringBootTest, @AutoConfigureMockMvc) with JSON fixtures, from the actual source code. Use when asked to write, add, or complete tests for REST endpoints or controllers.
---

# API REST Test

Act as a senior Java developer specializing in Spring Boot, REST APIs, JUnit 5, Mockito, and Spring MockMvc.

## Purpose

Analyze the provided source code and generate comprehensive tests for REST API endpoints, following the project's existing architecture, dependencies, coding conventions, and testing patterns: controller unit tests that isolate the HTTP layer, and integration tests that exercise the relevant application layers end to end.

## Scope

**In scope**

- Controller unit tests: one `@WebMvcTest` MVC-slice test class per relevant REST controller.
- REST API integration tests across request handling, validation, controller, service, mapping, persistence, and exception handling.
- JSON request/response fixtures, and any test configuration or database setup the tests need.
- A concise explanation of the covered scenarios and each test class's boundaries.
- A list of missing dependencies, source files, or API contract details that prevent complete test generation.

**Out of scope**

- Inventing application behavior, DTO fields, constructors, repository methods, or endpoint contracts that the source code does not show.
- Changing production code to make it testable; tests adapt to the existing code.
- Tests for non-REST components (services, repositories, messaging) outside an endpoint's integration boundary.
- Connecting to production systems or using production credentials.
- Claiming that tests pass without having executed them.

## Workflow

1. Analyze the supplied source code before generating tests: controllers (endpoint paths, HTTP methods, path variables, query parameters, headers, status codes), request/response DTOs and their constructors, services, mappers, repositories and entities, exception handlers, Spring Security configuration, existing tests, and the build file (Spring Boot version and test dependencies decide the annotations to use).
2. Write the controller unit tests (see **Controller unit tests**).
3. Write the integration tests (see **Integration tests**).
4. Add the JSON fixtures the tests read (see **JSON fixtures**).
5. Place and name classes so the two boundaries stay distinct (see **Test boundaries**).
6. Deliver everything listed in **Expected output**.
7. Check every item of the **Definition of done**; run the tests when possible and report the real result.

Do not invent application behavior, DTO fields, constructors, repository methods, or endpoint contracts. If the source code is incomplete, identify the missing information, list it in the output, and clearly state any assumptions instead of guessing.

## Controller unit tests

Test REST controllers in isolation to verify HTTP behavior, request handling, response mapping, and delegation to application services.

Start from the reference template in `references/controller-unit-test-template.md` and adapt it to the actual controller, method signatures, DTOs, endpoint paths, and HTTP status codes. Use the Spring testing annotations supported by the project's version.

**Mocking rules**

- Use `@WebMvcTest` (`@WebMvcTest(<Controller>.class)`) to load the controller's MVC slice only.
- Keep the controller and `MockMvc` real.
- Mock the controller's dependencies, such as services and mappers, with `@MockitoBean` (Spring Boot 3.4+); fall back to the deprecated `@MockBean` only when the project's version lacks `@MockitoBean`.
- Stub only the dependencies needed for the scenario.
- Verify service calls and mapping interactions when relevant, with the expected arguments.
- Assert actual HTTP responses for validation and exception-handling scenarios.
- Respect the project's Spring Security and exception-handling configuration.

**Verify, where applicable**

- Correct HTTP method, endpoint path, and response status.
- Request body deserialization and response serialization.
- Response content type and JSON structure.
- Correct delegation to services with expected arguments.
- Correct mapping between domain objects and response DTOs.
- Request validation failures, including missing or invalid fields.
- Malformed JSON and unsupported content types.
- Business exceptions, resource-not-found errors, and conflict responses.
- Query parameters, path variables, headers, and optional request fields.
- Authentication and authorization behavior.
- Empty responses, including `204 No Content`, where applicable.

## Integration tests

Verify that REST endpoints work across the relevant application layers, including request handling, validation, controller logic, service logic, mapping, persistence, and exception handling.

Start from the illustrative template in `references/integration-test-template.md` and replace its placeholders with the actual repositories, entities, and application configuration from the supplied source code.

Do not introduce repository dependencies if the endpoint does not use persistence.

**Integration testing rules**

- Use `@SpringBootTest` with `@AutoConfigureMockMvc` when testing through MockMvc.
- Preserve the project's existing integration testing approach if it uses a real HTTP client or a random-port server.
- Use real application services, mappers, repositories, and other collaborators for the layers under test.
- Mock only external dependencies intentionally excluded from the integration boundary.
- Use the configured test database or an isolated database environment.
- Prepare test data explicitly and prevent tests from depending on execution order.
- Use Testcontainers or existing infrastructure when actual database or infrastructure behavior must be verified.
- Never connect tests to production systems or use production credentials.
- Verify observable outcomes rather than relying solely on internal method calls.
- Check database state when persistence is part of the behavior being tested.
- Account for transaction boundaries, asynchronous processing, and external side effects where relevant.

**Integration scenarios to consider** (cover those the application supports)

- Creating, retrieving, updating, and deleting resources.
- Persisting and retrieving data correctly.
- Handling resources that do not exist.
- Validation errors and malformed requests.
- Business rule violations and duplicate resources.
- Transaction rollback and consistency where applicable.
- Pagination, sorting, and filtering.
- Authentication and authorization.
- Serialization and deserialization of complex payloads.
- Exception translation into the expected HTTP error responses.
- Failures from external systems, where included in the integration scope.

## JSON fixtures

Use JSON resource files for request and response payloads when appropriate and consistent with the project, for example:

```text
src/test/resources/json/
├── create-order-request.json
├── create-order-response.json
├── invalid-order-request.json
├── order-not-found-response.json
└── validation-error-response.json
```

- Load fixtures with `@Value("classpath:/json/<file>.json") Resource` and read them as UTF-8 (`getContentAsString(StandardCharsets.UTF_8)`).
- Match fixtures to the actual API contract and DTO definitions.
- Include the expected error structure when testing failures.
- Avoid hardcoded expectations for generated IDs, timestamps, or other unstable fields unless controlled by the test.
- Keep fixtures focused on the scenario being tested.
- Prefer direct assertions for simple responses when external fixtures would add unnecessary complexity.

## Test boundaries

Follow the project's existing package structure and naming conventions. Suggested class names: `<Name>ControllerTest` for controller unit tests and `<Name>ControllerIntegrationTest` for integration tests; keep an existing naming scheme when the project already has one.

Keep the two testing boundaries distinct:

| Aspect | Controller unit test | Integration test |
| --- | --- | --- |
| Spring context | MVC slice | Full application context |
| Controller | Real | Real |
| Service | Mocked | Real, unless explicitly excluded |
| Mapper | Mocked when appropriate | Real |
| Repository | Usually outside the test scope | Real when persistence is included |
| Primary assertions | HTTP response and delegation | HTTP response and end-to-end effects |
| Database | Not required | Test database when applicable |

## Expected output

After analyzing the supplied source code, generate:

1. A controller unit test class for each relevant REST controller.
2. Integration test classes covering the appropriate application layers.
3. The JSON fixtures required by the generated tests, including their exact resource paths.
4. Any necessary test configuration or database setup.
5. A concise explanation of the scenarios covered and the boundaries of each test class.
6. A list of missing dependencies, source files, or API contract details that prevent complete test generation.

## Rules

| ID  | Rule | Check |
| --- | ---- | ----- |
| R1  | The description states when to use the skill | static |
| R2  | The skill ID is lowercase kebab-case and equals the directory name | static |
| R3  | The source code is analyzed before any test is generated | static |
| R4  | Application behavior, DTO fields, constructors, repository methods, and endpoint contracts are never invented | static |
| R5  | Controller unit tests load only the MVC slice with `@WebMvcTest`, keeping the controller and MockMvc real | static |
| R6  | Controller dependencies are mocked with `@MockitoBean` or the project's supported annotation | static |
| R7  | Only the dependencies needed for the scenario are stubbed | static |
| R8  | Service delegation and mapping interactions are verified with the expected arguments | static |
| R9  | Controller unit tests cover the full HTTP verification checklist | static |
| R10 | Validation and exception scenarios assert actual HTTP responses and respect security and exception-handling configuration | static |
| R11 | The controller unit test template is provided and adapted to the actual controller | static |
| R12 | Integration tests use `@SpringBootTest` with `@AutoConfigureMockMvc` unless the project uses a real HTTP client or random port | static |
| R13 | Integration tests use real collaborators and mock only excluded external dependencies | static |
| R14 | Integration test data is explicit, isolated, and independent of execution order | static |
| R15 | Tests never connect to production systems or use production credentials | static |
| R16 | Integration tests verify observable outcomes, including database state when persistence is involved | static |
| R17 | No repository dependency is introduced when the endpoint does not use persistence | static |
| R18 | The integration scenario list is considered | static |
| R19 | The integration test template is provided and adapted to the actual repositories and configuration | static |
| R20 | JSON fixtures live under `src/test/resources/json/` and are read as UTF-8 | static |
| R21 | Fixtures match the actual API contract, include error structures, and avoid unstable fields | static |
| R22 | Direct assertions are preferred for simple responses | static |
| R23 | Unit and integration test boundaries stay distinct per the boundary table | static |
| R24 | Test classes follow the project's packages and `ControllerTest` / `ControllerIntegrationTest` naming | static |
| R25 | The output lists the six expected deliverables | static |
| R26 | Assumptions and unverified behavior are explicitly identified | static |
| R27 | Tests are never claimed to pass unless their execution has been verified | static |
| R28 | The definition of done requires actual contracts, isolation, real collaborators, failure coverage, side effects, fixtures, and compilation | static |
| R29 | The Rules table lists exactly the spec IDs | static |
| R30 | The skill ships a README.md documentation file | static |
| R31 | A controller unit test request yields a `@WebMvcTest` slice with mocked services, not a full-context test | agent-evaluation |
| R32 | Missing source code is reported as missing information instead of invented contracts | agent-evaluation |

## Acceptance criteria

The skill has executed successfully when every item of the definition of done holds.

### Definition of done

- [ ] Tests use the actual endpoint paths, HTTP methods, DTOs, constructors, and response contracts.
- [ ] Unit tests isolate the controller's dependencies.
- [ ] Integration tests exercise the intended application layers using real collaborators.
- [ ] Success and relevant failure responses are verified.
- [ ] Persistence and other observable side effects are checked where applicable.
- [ ] Required fixtures and test configuration are included.
- [ ] Tests follow existing project conventions and compile against the project's actual dependencies.
- [ ] Any unverified behavior or assumptions are explicitly identified.
- [ ] Do not claim tests pass unless their execution has been verified.
