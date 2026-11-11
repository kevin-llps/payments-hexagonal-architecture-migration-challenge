---
name: Dependency Inversion Principle
description: Apply the Dependency Inversion Principle before creating classes and wiring dependencies. Use when designing or implementing a feature whose business logic needs databases, external APIs, messaging, files, or other low-level mechanisms, to decide abstractions, dependency direction, and object construction first.
---

# Dependency Inversion Principle

## Invocation Announcement

Before any other output or tool call, display exactly this line as the skill's first output:

`▶ Skill invoked: Dependency Inversion Principle — decide abstractions and dependency direction before creating classes`

Display it on every invocation, whether triggered by the user, an agent, or a skill. When delegating to a subagent or loading a skill, do not print its announcement on its behalf; it prints its own. The announcement never replaces or delays a mandatory approval gate.

Design dependency direction before writing classes: high-level business logic
depends on abstractions it owns, and low-level mechanisms satisfy them.

## Purpose

Apply the Dependency Inversion Principle (DIP) before creating classes and dependencies to ensure that high-level business logic does not depend directly on low-level implementation details.

## Scope

**In scope**

- Analyzing the dependency relationships required by the feature.
- Identifying high-level policies and low-level technical mechanisms.
- Defining appropriate abstractions at the boundaries between them.
- Designing dependency direction and object construction before implementation.

**Out of scope**

- The other SOLID principles (single-responsibility, open-closed,
  interface-segregation, Liskov substitution); they have their own skills.
- Changing unrelated architectural boundaries or reworking the project's
  established dependency injection and configuration conventions.
- Introducing a new dependency injection framework.
- Cleaning up or refactoring existing code that the feature does not touch.

## Design guidelines

### Identify dependency boundaries

Before creating classes:

- Identify the high-level business rules that implement the feature.
- Identify low-level mechanisms such as database access, external APIs, messaging, and file operations.
- Determine where concrete dependencies would make business logic difficult
  to change, isolate, or test.
- Distinguish dependencies that need abstraction from simple implementation
  details that do not justify additional complexity.

### Define appropriate abstractions

- Make high-level modules depend on abstractions representing the
  capabilities they require.
- Define interfaces or other suitable contracts around meaningful business or
  application needs.
- Ensure abstractions are owned by the layer that needs the capability when
  consistent with the project's architecture (for example, a port declared in
  the application or domain layer, not in the infrastructure package).
- Avoid exposing infrastructure-specific types in high-level business contracts
  (for example `EntityManager`, `JdbcTemplate`, `KafkaTemplate`, HTTP clients,
  JPA entities) when they are not part of the required behavior.

### Design dependency direction

- Ensure high-level business logic depends on contracts rather than directly
  constructing low-level implementations (no `new` of infrastructure inside
  business logic).
- Make infrastructure components implement or satisfy the abstractions
  required by higher-level components, so source-code dependencies point from
  infrastructure toward the abstraction.
- Use constructor injection or the project's established dependency injection
  mechanism.
- Keep object creation and infrastructure configuration outside business logic
  where appropriate (composition root, configuration classes).
- Avoid service locators, hidden global dependencies, and unnecessary static access to collaborators.

### Preserve simplicity

- Introduce abstractions where they establish a meaningful boundary, improve
  replaceability, or isolate volatile infrastructure.
- Do not create interfaces for every class automatically. Stable, pure helpers
  and value objects are used directly.
- Avoid excessive indirection and unnecessary dependency injection frameworks.
- Follow the existing architecture and established dependency management
  patterns.

## Workflow

Run these steps before implementation, in order:

1. **Analyze** — identify high-level policies, low-level mechanisms, and their
   relationships.
2. **Map** — determine which components need which capabilities.
3. **Design** — define the abstractions and the dependency direction.
4. **Validate** — check that business logic does not depend unnecessarily on
   concrete infrastructure implementations.
5. **Present** — explain the proposed dependency graph and object construction
   strategy (the expected deliverables below).
6. **Implement** — create the abstractions, implementations, and dependency
   wiring only after validating the design against DIP.

## Constraints

- Do not create classes or wire dependencies before analyzing dependency direction.
- Do not introduce abstractions without a concrete design benefit.
- Do not allow infrastructure concerns to leak into business logic
  unnecessarily.
- Do not change unrelated architectural boundaries.
- Follow the project's existing dependency injection
  and configuration conventions.

## Expected deliverables

Before creating classes, provide:

1. The identified high-level and low-level components.
2. The proposed abstractions and the capabilities they expose.
3. The dependency relationships between components.
4. The intended dependency injection and object construction strategy.
5. An explanation of how the design reduces coupling and supports isolated testing.

## Rules

| ID  | Rule                                                                              | Check            |
| --- | --------------------------------------------------------------------------------- | ---------------- |
| R1  | The skill is discoverable and its description says when to use it                 | static           |
| R2  | DIP is applied before creating classes and dependencies                           | static           |
| R3  | Scope lists in-scope and out-of-scope items, excluding sibling SOLID principles   | static           |
| R4  | High-level policies and low-level mechanisms are identified first                 | static           |
| R5  | Simple details that do not justify abstraction are distinguished                  | static           |
| R6  | High-level modules depend on abstractions owned by the consuming layer            | static           |
| R7  | Infrastructure-specific types stay out of high-level business contracts           | static           |
| R8  | Infrastructure implements abstractions; constructor injection wires them          | static           |
| R9  | No service locators, hidden globals, or unnecessary static access                 | static           |
| R10 | No interface per class, no excessive indirection or needless DI frameworks        | static           |
| R11 | Workflow: Analyze, Map, Design, Validate, Present, Implement                      | static           |
| R12 | No class or wiring before dependency direction is analyzed                        | static           |
| R13 | No abstraction without a concrete design benefit                                  | static           |
| R14 | Unrelated boundaries unchanged; existing DI/configuration conventions followed    | static           |
| R15 | The five design deliverables are presented before creating classes                | static           |
| R16 | Acceptance criteria state the definition of done                                  | static           |
| R17 | Rules table matches the spec and a README ships beside SKILL.md                   | static           |
| R18 | Infrastructure needs get a consumer-owned abstraction and a design before code    | agent-evaluation |
| R19 | A simple stable collaborator is not wrapped in a needless interface               | agent-evaluation |
| R20 | The first output line announces the skill invocation                              | static           |

## Acceptance criteria

- [ ] The five expected deliverables were presented before any class was
      created or dependency wired.
- [ ] High-level business logic depends on appropriate abstractions owned by
      the layer that needs them, with no infrastructure-specific types in
      their contracts.
- [ ] The low-level implementations satisfy those abstractions.
- [ ] Dependencies are wired through the established architecture
      (constructor injection or the project's DI mechanism), with no service
      locator, hidden global, or unnecessary static access.
- [ ] Unnecessary coupling is avoided and no abstraction exists without a
      concrete design benefit.
- [ ] The first output line is `▶ Skill invoked: Dependency Inversion Principle — decide abstractions and dependency direction before creating classes`.
