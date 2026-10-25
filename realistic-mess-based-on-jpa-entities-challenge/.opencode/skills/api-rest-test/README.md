# API REST Test

Generates Spring Boot REST API tests from the actual source code: controller unit tests that load only the MVC slice (`@WebMvcTest`, real `MockMvc`, services and mappers mocked with `@MockitoBean`) and integration tests that run the full application context (`@SpringBootTest` + `@AutoConfigureMockMvc`) against real collaborators and a test database, plus the JSON fixtures they read. It never invents endpoint contracts and never claims tests pass without running them.

## When to use

Use when asked to write, add, or complete tests for REST endpoints or controllers in a Spring Boot project.

## How to run

Ask for REST API tests and point at the controllers to cover, for example: "Use the api-rest-test skill to write controller unit tests and integration tests for `InstantPaymentController`." Supply or let the agent read the controller, DTO, service, mapper, repository, and exception-handler sources.

## Dependency graph

No agent or skill calls this skill yet; it is loaded directly from a user request.

```mermaid
graph TD
    S["api-rest-test"]
```

## Workflow graph

```mermaid
graph TD
    S1["1: Analyze the supplied source code and record missing information"] --> S2["2: Write controller unit tests - WebMvcTest slice, mocked services"]
    S2 -.-> F1["builds: src/test/java/.../<Name>ControllerTest.java"]
    S2 --> S3["3: Write integration tests - full context, real collaborators"]
    S3 -.-> F2["builds: src/test/java/.../<Name>ControllerIntegrationTest.java"]
    S3 --> S4["4: Add JSON fixtures"]
    S4 -.-> F3["builds: src/test/resources/json/*.json"]
    S4 --> S5["5: Keep unit and integration boundaries distinct"]
    S5 --> S6["6: Deliver the expected output"]
    S6 --> S7["7: Check the definition of done and report real test results"]
```
