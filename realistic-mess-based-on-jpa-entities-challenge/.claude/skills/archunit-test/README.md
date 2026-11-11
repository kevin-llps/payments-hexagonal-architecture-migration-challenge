# ArchUnit Test

Writes and maintains ArchUnit tests that enforce the hexagonal architecture
rules of a Java/Maven multi-module project at build time. The tests go in the
composition-root module (`bootstrap`), whose test classpath sees every
production class. The skill holds **how** to write the tests. The calling agent
decides **when** they are written.

## When to use

Use when writing, adding, or tightening ArchUnit architecture tests in the
composition-root module.

## How to run

Ask the agent to "use the `archunit-test` skill" and give four inputs: the
target module (default `bootstrap`), the base package (e.g.
`fr.kevin.llps.payments`), the rule sources (the approved design documents),
and the rules to add or tighten. If any input is missing, the skill asks for it
in one batched question.

## Dependency graph

```mermaid
graph TD
    C1["1: incremental-hexagonal-architecture-migration writes and extends the ArchUnit tests in bootstrap"] --> S["archunit-test"]
```

## Workflow graph

```mermaid
graph TD
    S1["1: Collect the required inputs, asking once for all missing ones"] --> S2["2: Read the rule sources and existing ArchUnit tests, resolve real package names"]
    S2 --> S3["3: Check the dependency setup and add it if missing"]
    S3 -.-> F1["builds: parent pom.xml dependencyManagement and &lt;target-module&gt;/pom.xml test dependency"]
    S3 --> S4["4: List the rules to add or tighten as a catalogue"]
    S4 --> S5["5: Write each rule in its rule-family class with a because reason"]
    S5 -.-> F2["builds: &lt;target-module&gt;/src/test/java/&lt;base-package-path&gt;/architecture/*Test.java"]
    S5 --> S6["6: Run the ArchUnit tests and report RED/GREEN per rule and any exception"]
    S6 --> S7["7: Return the catalogue, the files changed, and the test results"]
```
