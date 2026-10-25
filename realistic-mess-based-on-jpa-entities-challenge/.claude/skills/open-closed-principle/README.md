# Open/Closed Principle

Applies the Open/Closed Principle before any class is created: it separates
stable behavior from behavior likely to vary, adds extension points only where
a real variation justifies them, and presents the design before implementing
it, so new behavior can be added without modifying stable code and without
speculative abstractions.

## When to use

Use when designing or creating new classes for a feature, adding a new variant
of an existing behavior, or reviewing a proposed design for extensibility
before implementation.

## How to run

Load the `open-closed-principle` skill, then ask for a design, for example:
"Using the open-closed-principle skill, design the classes for <feature>
before writing any code."

## Dependency graph

No agent or skill calls this skill yet; it is loaded directly by the user or
by the agent performing the design.

## Workflow graph

```mermaid
graph TD
    S1["1: Analyze - stable behavior and potential variations"] --> S2["2: Classify - which behaviors need independent extension"]
    S2 --> S3["3: Design - classes, contracts, extension points"]
    S3 --> S4["4: Validate - no unnecessary change to stable code, no speculative abstraction"]
    S4 --> S5["5: Present - the five expected deliverables"]
    S5 --> S6["6: Implement - create the classes after validation"]
    S6 -.-> F1["builds: the designed classes in the project's existing structure"]
```
