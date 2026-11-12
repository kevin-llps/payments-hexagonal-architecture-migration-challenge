---
name: Dependency Inversion Principle
description: Apply the Dependency Inversion Principle (DIP) so high-level business logic depends on abstractions rather than on low-level implementation details. Use before creating classes and wiring their dependencies, when designing ports, adapters, services, or any business logic that needs databases, external APIs, messaging, or files.
---

# Dependency Inversion Principle

## Invocation Announcement

Before any other output or tool call, display exactly this line as the skill's first output:

`▶ Skill invoked: Dependency Inversion Principle — decide abstractions and dependency direction before creating classes`

Display it on every invocation, whether triggered by the user, an agent, or a skill. When delegating to a subagent or loading a skill, do not print its announcement on its behalf; it prints its own. The announcement never replaces or delays a mandatory approval gate.

## Purpose

Apply the Dependency Inversion Principle before creating classes and
dependencies, so that high-level business logic does not depend directly on
low-level implementation details.

## Scope

**In scope**

- Analyzing the dependency relationships required by the feature.
- Identifying high-level policies and low-level technical mechanisms.
- Defining appropriate abstractions at the boundaries between them.
- Designing dependency direction and object construction before
  implementation.

**Out of scope**

- Applying the other SOLID principles (single responsibility, open/closed,
  Liskov substitution, interface segregation); each has its own skill.
- Changing unrelated architectural boundaries.
- Introducing an abstraction that has no concrete design benefit.

## Design guidelines

### Identify dependency boundaries

Before creating classes:

- Identify the high-level business rules that implement the feature.
- Identify low-level mechanisms such as database access, external APIs,
  messaging, and file operations.
- Determine where concrete dependencies would make business logic difficult to
  change, isolate, or test.
- Distinguish dependencies that need abstraction from simple implementation
  details that do not justify additional complexity (stable, pure in-process
  helpers, value objects, and utility code without I/O usually stay concrete).

### Define appropriate abstractions

- Make high-level modules depend on abstractions representing the capabilities
  they require (name them after the capability, for example
  `LoadPaymentPort`, not after the technology, for example `JpaPaymentDao`).
- Define interfaces or other suitable contracts around meaningful business or
  application needs.
- Ensure abstractions are owned by the layer that needs the capability when
  consistent with the project's architecture (in a hexagonal layout, ports
  live in the domain or application module, never in the adapter module).
- Avoid exposing infrastructure-specific types in high-level business contracts
  when they are not part of the required behavior (for example
  `EntityManager`, JPA entities, `KafkaTemplate`, `ResultSet`, HTTP clients,
  or topic and table names); use domain types instead.

### Design dependency direction

- Ensure high-level business logic depends on contracts rather than directly
  constructing low-level implementations (no `new JdbcRepository()` or
  `Persistence.createEntityManagerFactory(...)` inside a service).
- Make infrastructure components implement or satisfy the abstractions
  required by higher-level components, so source-code dependencies point from
  infrastructure toward the business layer.
- Use constructor injection or the project's established dependency injection
  mechanism.
- Keep object creation and infrastructure configuration outside business logic
  where appropriate (for example in a composition root or configuration class).
- Avoid service locators, hidden global dependencies, and unnecessary static
  access to collaborators (for example `ApplicationContext.getBean(...)` or
  static factory calls inside business logic).

### Preserve simplicity

- Introduce abstractions where they establish a meaningful boundary, improve
  replaceability, or isolate volatile infrastructure.
- Do not create interfaces for every class automatically: DIP inverts
  dependencies on volatile details, it does not require an interface per
  class. A stable concrete collaborator can still be injected through the
  constructor without an interface.
- Avoid excessive indirection (single-implementation interfaces plus factories
  with no benefit) and unnecessary dependency injection frameworks.
- Follow the existing architecture and established dependency management
  patterns.

## Workflow

Run these steps before implementation, in order:

1. **Analyze:** Identify high-level policies, low-level mechanisms, and their
   relationships.
2. **Map:** Determine which components need which capabilities.
3. **Design:** Define the abstractions and dependency direction.
4. **Validate:** Check that business logic does not depend unnecessarily on
   concrete infrastructure implementations, and that no abstraction was added
   without a concrete design benefit.
5. **Present:** Explain the proposed dependency graph and object construction
   strategy, using the deliverables below.
6. **Implement:** Create the abstractions, implementations, and dependency
   wiring only after validating the design against DIP.

## Constraints

- Do not create classes or wire dependencies before analyzing dependency
  direction.
- Do not introduce abstractions without a concrete design benefit.
- Do not allow infrastructure concerns to leak into business logic
  unnecessarily.
- Do not change unrelated architectural boundaries.
- Follow the project's existing dependency injection and configuration
  conventions.

## Expected deliverables

Before creating classes, provide:

1. The identified high-level and low-level components.
2. The proposed abstractions and the capabilities they expose.
3. The dependency relationships between components.
4. The intended dependency injection and object construction strategy.
5. An explanation of how the design reduces coupling and supports isolated
   testing.

## Acceptance criteria

Definition of done:

- [ ] High-level business logic depends on appropriate abstractions.
- [ ] Low-level implementations satisfy those abstractions.
- [ ] Dependencies are wired through the established architecture.
- [ ] No dependency adds coupling without need: unnecessary coupling is
      avoided, and no abstraction was introduced without a concrete design
      benefit.
- [ ] The five deliverables were presented before any class was created.
- [ ] The first output line is `▶ Skill invoked: Dependency Inversion Principle — decide abstractions and dependency direction before creating classes`.

## Rules

| ID  | Rule                                                                                         | Check            |
| --- | -------------------------------------------------------------------------------------------- | ---------------- |
| R1  | The skill is discoverable and its description says when to use it                           | static           |
| R2  | High-level business rules and low-level mechanisms are identified before any class          | static           |
| R3  | Dependencies needing an abstraction are distinguished from simple implementation details    | static           |
| R4  | High-level modules depend on abstractions shaped around the capabilities they require       | static           |
| R5  | An abstraction is owned by the layer that needs the capability                              | static           |
| R6  | High-level contracts expose no unnecessary infrastructure-specific types                     | static           |
| R7  | Business logic depends on contracts; infrastructure implements them                          | static           |
| R8  | Constructor injection or the established mechanism; object creation outside business logic | static           |
| R9  | No service locators, hidden global dependencies, or unnecessary static access               | static           |
| R10 | Simplicity: meaningful boundaries only, no interface per class, no excessive indirection    | static           |
| R11 | Workflow: Analyze, Map, Design, Validate, Present, Implement                                 | static           |
| R12 | Constraints on analysis, benefit, leaks, unrelated boundaries, and conventions               | static           |
| R13 | The five deliverables are presented before creating classes                                  | static           |
| R14 | The definition of done is stated as checkable acceptance criteria                            | static           |
| R15 | A Rules table covers every spec ID and a README ships with the skill                         | static           |
| R16 | A service constructing infrastructure gets a layer-owned port wired by constructor injection | agent-evaluation |
| R17 | A stable, pure in-process helper stays a concrete dependency without an interface           | agent-evaluation |
| R18 | The first output line announces the skill invocation                                         | static           |
