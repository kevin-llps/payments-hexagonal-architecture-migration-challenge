# Single Responsibility Principle

Applies the Single Responsibility Principle before any class is created, so
each proposed class has one clearly defined responsibility and one primary
reason to change. It analyzes the feature, decomposes it into
responsibilities, proposes and validates class boundaries, and presents the
design before implementation, while avoiding overengineering.

## When to use

Use when designing or creating new classes for a feature, before writing any
implementation.

## How to run

Ask the agent to apply the `single-responsibility-principle` skill to a
feature, e.g. "Using the single-responsibility-principle skill, design the
classes for <feature>". The agent presents the SRP design deliverables before
creating any class.

## Dependency graph

No agent or skill calls this skill yet; it is loaded directly on request.

```mermaid
graph TD
    S["single-responsibility-principle"]
```

## Workflow graph

```mermaid
graph TD
    S1["1: Analyze - understand the requirements and identify distinct responsibilities"] --> S2["2: Decompose - group related behaviors and data by responsibility"]
    S2 --> S3["3: Design - propose the classes needed for the feature"]
    S3 --> S4["4: Validate - check each class against SRP and its reasons to change"]
    S4 --> S5["5: Present - responsibilities, classes, behaviors and data, collaboration, SRP justification, open questions"]
    S5 --> S6["6: Implement - create the classes only after the SRP evaluation"]
    S6 -.-> F1["builds: the feature's classes, following project conventions"]
```
