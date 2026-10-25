# Test Driven Development

Drives the development of a single public Java method by tests: the
acceptance criteria become atomic rules, and each rule is written as a failing
test (RED), implemented, and proven passing (GREEN) before the next one starts.

## When to use

Use when a feature must be implemented with TDD from a feature description and
its acceptance criteria, scoped to one public method.

## How to run

Ask the agent to "use the `tdd` skill" (or "implement this with TDD") and give
the feature description, the acceptance criteria, and the target class or
method. Without both the description and the criteria the skill only asks for
them.

## Dependency graph

No agent or skill calls `tdd` yet; it is invoked directly by the user.

## Workflow graph

```mermaid
graph TD
    S1["1: Intake - feature description and acceptance criteria, one public method"] --> S2["2: Decompose - acceptance criteria into atomic rules R1, R2, ..."]
    S2 --> S3["3: Signature - public method returning null, compiles"]
    S3 -.-> F1["builds: production class with the method signature"]
    S3 --> S4["4: Per-rule loop - test first, RED, implement, GREEN"]
    S4 -.-> F2["builds: one test method per rule in the test class"]
    S4 -.-> F3["builds: method implementation"]
    S4 --> S5["5: Iterate on all atomic rules"]
    S5 --> S4
    S5 --> S6["6: Show all test rule results with RED and GREEN evidence"]
```
