# Dependency Inversion Principle

Applies the Dependency Inversion Principle before classes are created: it
separates high-level business policies from low-level mechanisms, defines
abstractions owned by the consuming layer, and fixes dependency direction and
constructor-injection wiring before any code is written.

## When to use

Use when designing or implementing a feature whose business logic needs
databases, external APIs, messaging, files, or other low-level mechanisms, to
decide abstractions, dependency direction, and object construction first.

## How to run

Ask the agent to design or implement a feature "applying the
`dependency-inversion-principle` skill"; it presents the design deliverables
before creating any class.

## Dependency graph

No agent or skill calls this skill yet; it is loaded directly by the user's
request.

## Workflow graph

```mermaid
graph TD
    S1["1: Analyze - identify high-level policies and low-level mechanisms"] --> S2["2: Map - which components need which capabilities"]
    S2 --> S3["3: Design - abstractions and dependency direction"]
    S3 --> S4["4: Validate - no unnecessary dependency on concrete infrastructure"]
    S4 --> S5["5: Present - dependency graph and object construction strategy"]
    S5 --> S6["6: Implement - abstractions, implementations, and wiring"]
    S6 -.-> F1["builds: abstractions, adapters, and DI configuration of the feature"]
```
