---
name: Open/Closed Principle
description: Apply the Open/Closed Principle before creating classes, identifying which behaviors may vary and designing justified extension points without speculative abstractions. Use when designing or creating new classes for a feature, adding a new variant of an existing behavior, or reviewing a proposed design for extensibility before implementation.
---

# Open/Closed Principle

## Invocation Announcement

Before any other output or tool call, display exactly this line as the skill's first output:

`▶ Skill invoked: Open/Closed Principle — design justified extension points for behaviors that may vary`

Display it on every invocation, whether triggered by the user, an agent, or a skill. When delegating to a subagent or loading a skill, do not print its announcement on its behalf; it prints its own. The announcement never replaces or delays a mandatory approval gate.

## Purpose

Apply the Open/Closed Principle before creating classes, so that the system
lets new behaviors be added without modifying existing, stable code
unnecessarily. Software entities stay open for extension and closed for
modification only where a real, identified variation justifies it.

## Scope

**In scope**

- Analyzing feature requirements before designing or creating classes.
- Identifying behaviors that are likely to evolve or require additional
  implementations.
- Defining extension points where new behavior may be introduced.
- Avoiding unnecessary abstractions when the requirements do not justify them.

**Out of scope**

- The other SOLID principles (Single Responsibility, Interface Segregation,
  Liskov Substitution, Dependency Inversion); each has its own skill. Mention
  an issue that belongs to them only when it blocks the OCP design.
- Refactoring existing code, or "cleaning up" legacy code, that the requested
  feature does not need to change. Existing behavior is preserved as-is.
- Introducing abstractions for behavior with no identified variation.

## Design guidelines

### Identify potential variation

Before creating classes:

- Identify business rules and behaviors that may vary independently.
- Determine whether new implementations, business rules, or processing
  strategies may be introduced, and what evidence says so (an existing second
  variant, a stated requirement, a scheduled change).
- Distinguish stable behavior from behavior likely to evolve.
- Do not introduce extension points based solely on hypothetical future requirements.
  A variation needs evidence before it earns an abstraction.

### Design for extension

- Use interfaces, abstract classes, composition, or suitable design patterns
  (strategy, template method, decorator, registry of handlers) only when they
  provide a meaningful extension mechanism.
- Define stable contracts that allow new implementations to be introduced
  without changing existing consumers.
- Prefer polymorphism and composition over conditional logic that must
  continually be modified to support new variants.
- Keep extension points focused on a specific variation in behavior: one
  contract per axis of change, not one contract for everything that might move.

### Minimize modifications to existing code

- Design new behavior to be introduced through new implementations or components
  whenever appropriate.
- Avoid repeatedly modifying existing classes to add new variants of the same
  behavior.
- Ensure that adding a new implementation does not require unnecessary changes
  to unrelated classes.
- Do not eliminate all conditional logic indiscriminately; use abstractions
  only when they improve maintainability. A single, stable `if` is fine.

### Avoid overengineering

- Do not create interfaces or abstractions without a concrete design need.
- Do not anticipate every conceivable future variation.
- Prefer a straightforward implementation when the behavior is stable and
  unlikely to require independent extension. It can be opened for extension
  later, when a real second variant appears.

## Workflow

Run these steps before implementation, in order:

1. **Analyze** — read the feature requirements and identify stable behavior
   and potential variations, with the evidence for each variation.
2. **Classify** — decide which behaviors need independent extension and which
   stay as straightforward code.
3. **Design** — define the classes, contracts, and extension points required,
   following the design guidelines above.
4. **Validate** — check whether adding a new behavior would require
   unnecessary modifications to existing stable code or unrelated classes, and
   whether any abstraction lacks a concrete need. Revise the design if so.
5. **Present** — explain the proposed design with the expected deliverables
   below and why each extension mechanism is justified (or why none is).
6. **Implement** — create the classes only after validating the design against OCP.

## Constraints

- Do not create or modify classes before evaluating extension requirements.
- Do not introduce abstractions solely to satisfy the principle mechanically.
- Do not modify unrelated behavior or introduce unnecessary architectural
  complexity.
- Follow the project's existing architecture and conventions (package layout,
  naming, layering, test style); place new contracts where the project already
  places them.

## Expected deliverables

Before creating classes, provide:

1. The stable behaviors and potential variations identified.
2. The proposed classes and their responsibilities.
3. The interfaces, abstractions, or composition mechanisms required, if any ("none" is a valid answer).
4. An explanation of how new behavior can be added with minimal changes to existing code.
5. Any assumptions or unresolved design questions.

## Rules

| ID  | Rule                                                                                         | Check            |
| --- | -------------------------------------------------------------------------------------------- | ---------------- |
| R1  | The skill is discoverable, its ID is kebab-case, and its description says when to use it     | static           |
| R2  | The purpose is to apply OCP before creating classes so new behavior needs no stable-code edit | static           |
| R3  | Other SOLID principles and unrequested refactoring of existing code are out of scope         | static           |
| R4  | Stable behavior is distinguished from behavior likely to evolve before any class is created  | static           |
| R5  | Extension points are never based solely on hypothetical future requirements                  | static           |
| R6  | Extension uses stable contracts; polymorphism/composition over ever-changing conditionals    | static           |
| R7  | Each extension point is focused on one specific variation in behavior                        | static           |
| R8  | New variants come through new implementations without changes to unrelated classes           | static           |
| R9  | Conditional logic is not eliminated indiscriminately                                         | static           |
| R10 | No abstraction without a concrete design need; stable behavior stays straightforward         | static           |
| R11 | The workflow runs Analyze, Classify, Design, Validate, Present, Implement in order            | static           |
| R12 | No class is created or modified before extension requirements are evaluated                  | static           |
| R13 | No mechanical abstractions, no unrelated changes, project conventions followed               | static           |
| R14 | The five design deliverables are provided before creating classes                            | static           |
| R15 | Done means justified extensions without speculative abstractions                             | static           |
| R16 | A README documents purpose, usage, dependency graph and workflow graph                       | static           |
| R17 | Stable single-implementation behavior gets no speculative interface                          | agent-evaluation |
| R18 | Growing variants get a focused extension point, presented before any class is written        | agent-evaluation |
| R19 | The first output line announces the skill invocation                                         | static           |

## Acceptance criteria

Definition of done: the design supports justified behavioral extensions
without unnecessary modification of existing stable code and avoids
speculative abstractions.

- [ ] Stable behaviors and potential variations are listed before any class is
      created or modified.
- [ ] Every extension point is backed by an identified variation, not by a
      hypothetical future requirement.
- [ ] Adding a new variant requires a new implementation, not edits to
      existing stable classes or unrelated classes.
- [ ] Stable behavior with no identified variation is implemented
      straightforwardly, without speculative abstractions.
- [ ] The five expected deliverables are presented before implementation.
- [ ] The first output line is `▶ Skill invoked: Open/Closed Principle — design justified extension points for behaviors that may vary`.
