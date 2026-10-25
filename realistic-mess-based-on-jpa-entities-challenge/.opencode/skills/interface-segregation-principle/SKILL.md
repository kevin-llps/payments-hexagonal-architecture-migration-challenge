---
name: Interface Segregation Principle
description: Apply the Interface Segregation Principle (ISP) so each interface exposes only the operations its consumers need. Use when designing, creating, or splitting interfaces (ports, contracts, abstractions) and their implementations, before writing them.
---

# Interface Segregation Principle

## Purpose

Apply the Interface Segregation Principle before creating interfaces and their
implementations, so that each interface exposes only the operations relevant
to its intended consumers.

## Scope

**In scope**

- Analyzing the needs of each consumer before defining interfaces.
- Identifying groups of operations that belong together from the consumer's
  perspective.
- Designing focused contracts that avoid unnecessary dependencies on unrelated
  methods.
- Preserving clear responsibilities between interfaces and their
  implementations.

**Out of scope**

- Applying the other SOLID principles (single responsibility, open/closed,
  Liskov substitution, dependency inversion); each has its own skill.
- Creating an interface that has no identified consumer or no clear
  abstraction or architectural benefit.

## Design guidelines

### Identify consumer needs

Before creating interfaces:

- Identify the components that will consume each proposed abstraction.
- Determine which operations each consumer actually requires.
- Group operations according to their purpose and the needs of their
  consumers.
- Identify operations that force consumers to depend on behavior they never
  use.

### Design focused interfaces

- Define interfaces around cohesive consumer needs rather than grouping
  unrelated operations into one large contract.
- Keep each interface limited to a meaningful set of related operations.
- Ensure each interface has a clear purpose and an appropriate level of
  abstraction.
- Split interfaces when different consumers require distinct capabilities
  that can evolve independently.

### Define implementation responsibilities

- Ensure implementations provide the capabilities declared by their
  interfaces.
- Avoid forcing implementations to provide meaningless methods (empty bodies,
  dummy return values) or to throw `UnsupportedOperationException` merely to
  satisfy an oversized interface. Needing such a method is a signal to split
  the interface.
- Allow a class to implement multiple focused interfaces when it genuinely
  supports those capabilities.
- Avoid unnecessary delegation layers (adapters, wrappers, facades) introduced
  solely to compensate for poorly designed interfaces; fix the contract
  instead.

### Avoid excessive fragmentation

- Do not create a separate interface for every method by default.
- Do not split cohesive operations when consumers naturally need them together.
- Consider interface stability, consumer requirements, and maintainability
  when deciding how to divide contracts.

## Workflow

Run these steps before implementation, in order:

1. **Analyze:** Identify the consumers and the operations each one requires.
2. **Group:** Organize related operations into cohesive capability groups.
3. **Design:** Define the proposed interfaces and their contracts.
4. **Validate:** Check that consumers are not required to depend on irrelevant
   operations, that no implementation would need meaningless or unsupported
   methods, and that cohesive operations were not fragmented.
5. **Present:** Explain the purpose of each interface and which consumers need
   it, using the deliverables below.
6. **Implement:** Create interfaces and their implementations only after
   validating the design against ISP.

## Constraints

- Do not create interfaces before understanding their consumers.
- Do not introduce interfaces without a clear abstraction or architectural
  benefit.
- Do not combine unrelated responsibilities into a single contract for
  convenience.
- Follow the project's existing naming and architectural conventions.

## Expected deliverables

Before creating interfaces or implementing classes, provide:

1. The identified consumers and their required operations.
2. The proposed interfaces and their responsibilities.
3. The operations assigned to each interface.
4. The intended implementations and their supported capabilities.
5. An explanation of how the design avoids unnecessary consumer dependencies.

## Acceptance criteria

Definition of done:

- [ ] Each interface represents a cohesive contract.
- [ ] Consumers depend only on the capabilities they need.
- [ ] Implementations are not forced to support irrelevant operations (no
      meaningless methods, no `UnsupportedOperationException`).
- [ ] The five deliverables were presented before any interface or
      implementation was created.

## Rules

| ID  | Rule                                                                                   | Check            |
| --- | -------------------------------------------------------------------------------------- | ---------------- |
| R1  | The skill is discoverable and its description says when to use it                     | static           |
| R2  | Consumers and their required operations are identified before any interface            | static           |
| R3  | Interfaces are defined around cohesive consumer needs                                  | static           |
| R4  | Interfaces are split when consumers need distinct, independently evolving capabilities | static           |
| R5  | No meaningless methods or UnsupportedOperationException in implementations            | static           |
| R6  | A class may implement several focused interfaces it genuinely supports                 | static           |
| R7  | No delegation layer only to compensate for a poorly designed interface                 | static           |
| R8  | Excessive fragmentation is avoided                                                     | static           |
| R9  | Workflow: Analyze, Group, Design, Validate, Present, Implement                          | static           |
| R10 | Constraints on consumers, benefit, unrelated responsibilities, and conventions         | static           |
| R11 | The five deliverables are presented before creating interfaces or classes              | static           |
| R12 | The definition of done is stated as checkable acceptance criteria                      | static           |
| R13 | A Rules table covers every spec ID and a README ships with the skill                   | static           |
| R14 | A fat interface with divergent consumers is split without unsupported stubs            | agent-evaluation |
| R15 | Operations one consumer always needs together stay in one interface                    | agent-evaluation |
