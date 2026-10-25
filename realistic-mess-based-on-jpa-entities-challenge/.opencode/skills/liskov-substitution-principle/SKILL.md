---
name: Liskov Substitution Principle
description: Apply the Liskov Substitution Principle before creating class hierarchies or interface implementations. Use when designing or reviewing base types, interfaces, subclasses, overrides, or implementations, so every subtype can replace its declared base type without breaking expected behavior.
---

# Liskov Substitution Principle

## Purpose

Apply the Liskov Substitution Principle (LSP) before creating class
hierarchies or implementations, to ensure that any subtype can replace its
declared base type or interface implementation without violating expected
behavior.

## Scope

**In scope**

- Analyzing the behavioral contracts required by the feature.
- Identifying the expected behavior of base types and interfaces.
- Evaluating whether proposed implementations can honor those contracts.
- Preferring composition over inheritance when the required behavioral
  contract cannot be maintained.

**Out of scope**

- The other SOLID principles, each covered by its own skill:
  `single-responsibility-principle`, `open-closed-principle`,
  `interface-segregation-principle`, `dependency-inversion-principle`.
- Writing or generating classes before the LSP design has been validated;
  implementation only follows the workflow below.
- Changing the established project architecture or business rules to make a
  hierarchy fit; they are preserved as given.

## Design guidelines

### Define behavioral contracts

Before creating classes:

- Identify the preconditions, postconditions, invariants, and observable behavior
  expected from each abstraction.
- Define what consumers can legitimately expect from the base type or
  interface.
- Specify valid inputs, expected outputs, and error-handling behavior.
- Ensure that the contract describes behavior rather than
  implementation details.

### Evaluate substitutability

- Ensure every proposed implementation honors the contract of its base type or
  interface.
- Do not strengthen preconditions in a way that rejects
  inputs accepted by the base contract.
- Do not weaken postconditions or omit guarantees made by the base contract.
- Preserve required invariants and expected observable behavior.
- Ensure implementations handle errors and exceptional conditions
  consistently with the declared contract.

### Choose appropriate abstractions

- Use inheritance only when the subtype genuinely satisfies the behavioral
  expectations of the base type.
- Use interfaces when multiple implementations can honor the same contract.
- Prefer composition when two classes share implementation details but have
  incompatible behavioral expectations.
- Avoid inheritance based solely on code reuse or superficial similarities.

### Avoid misleading hierarchies

- Do not force unrelated behaviors into a common abstraction.
- Do not create implementations that require consumers to inspect concrete types
  to determine whether an operation is supported. A consumer-side `instanceof`
  check or cast before calling an operation is a substitutability failure.
- Avoid overrides that unexpectedly change the meaning of
  inherited operations (for example an override returning different results
  for the same inputs, or throwing where the base type succeeds).
- Define explicit contracts for operations that may fail or have restricted
  behavior, so the restriction is part of the abstraction rather than a
  surprise in one implementation.

## Workflow

Run these steps before implementation, in order:

1. **Analyze:** Identify the behavior expected from each proposed abstraction.
2. **Define:** Specify its contract, invariants, and guarantees.
3. **Evaluate:** Determine whether each proposed implementation can honor that
   contract.
4. **Design:** Choose inheritance, interfaces, or composition based on
   behavioral compatibility.
5. **Validate:** Review whether consumers can use every implementation
   interchangeably within the contract.
6. **Implement:** Create the classes and relationships only after validating the design against LSP.

## Constraints

- Do not create inheritance hierarchies before defining their
  behavioral contracts.
- Do not use inheritance merely to share code.
- Do not introduce unsupported assumptions about implementation
  behavior.
- Preserve the established project architecture and business rules.

## Expected deliverables

Before creating classes, provide:

1. The proposed abstractions and their behavioral contracts.
2. The guarantees and invariants consumers can rely on.
3. The proposed implementations and their compatibility with those contracts.
4. Any substitutability risks and how the design addresses them.
5. A justification for using inheritance, interfaces, or composition.

## Rules

| ID  | Rule                                                                                         | Check            |
| --- | -------------------------------------------------------------------------------------------- | ---------------- |
| R1  | The skill is discoverable and its description says when to use it                            | static           |
| R2  | Preconditions, postconditions, invariants and observable behavior are defined before classes | static           |
| R3  | The contract covers inputs, outputs and errors, and describes behavior, not implementation   | static           |
| R4  | Implementations never strengthen preconditions                                               | static           |
| R5  | Implementations never weaken postconditions and preserve invariants                          | static           |
| R6  | Errors are handled consistently with the declared contract                                   | static           |
| R7  | Inheritance only for genuine subtypes; interfaces when several implementations honor one contract | static      |
| R8  | Composition over inheritance for incompatible behavior; never inherit for code reuse alone    | static           |
| R9  | Consumers never inspect concrete types to know whether an operation is supported             | static           |
| R10 | No forced unrelated behaviors, no meaning-changing overrides, explicit contracts for restricted operations | static |
| R11 | Workflow: Analyze, Define, Evaluate, Design, Validate, Implement, implementation last          | static           |
| R12 | No hierarchy before its contract; no inheritance merely to share code                         | static           |
| R13 | No unsupported behavioral assumptions; architecture and business rules preserved              | static           |
| R14 | The five deliverables are provided before classes are created                                | static           |
| R15 | Done means every implementation honors its contract with no consumer workarounds             | static           |
| R16 | The skill stays LSP-only; other SOLID principles are out of scope                            | static           |
| R17 | The skill ships a README.md and a Rules table covering exactly the spec IDs                  | static           |
| R18 | A subtype that throws on a supported operation is flagged and redesigned before coding        | agent-evaluation |
| R19 | Inheritance requested only for code reuse is redirected to composition with a justification   | agent-evaluation |

## Acceptance criteria

- [ ] Every proposed abstraction has a written behavioral contract
      (preconditions, postconditions, invariants, error behavior) before any
      class is created.
- [ ] The five expected deliverables are provided before implementation.
- [ ] No implementation strengthens a precondition, weakens a postcondition,
      breaks an invariant, or changes the declared error behavior.
- [ ] Every inheritance, interface, or composition choice carries a
      behavioral justification; none is justified by code reuse alone.
- [ ] Definition of done: every proposed implementation can honor its
      abstraction's behavioral contract, and consumers do not need
      implementation-specific workarounds to use valid implementations
      correctly.
