# Skill Creator

Turns five user-provided inputs — responsibility, in-scope, out-of-scope,
instructions and acceptance criteria — into a discoverable, test-first OpenCode
skill. Atomic rules and their failing tests are written before `SKILL.md`, and
success is reported only once the rule tests are GREEN.

## When to use

Use when the user asks to create, define, or scaffold a new skill.

## How to run

Ask for a new skill through the `skill-forge` agent — for example
"create a skill called weather" — and provide the five inputs when asked. The
agent loads this skill for the intake and authoring steps.

## Dependency graph

```mermaid
graph TD
    A1["1: skill-forge agent - loads this skill for intake and authoring"] --> S["skill-creator"]
```

No other agent or skill calls `skill-creator`; `skill-updater` deliberately
never does, because updating an existing skill is not creation.

## Workflow graph

```mermaid
graph TD
    S1["1: Intake - collect the five inputs"] --> S2["2: Resolve the target - kebab-case ID"]
    S2 --> S3["3: Refuse collisions - creation is create-only"]
    S3 --> S4["4: Test-first - delegate rule IDs and tests to skill-testing"]
    S4 -.-> F1["builds: .opencode/skills/{id}/tests/rules.json"]
    S4 --> S5["5: Author - SKILL.md and README.md"]
    S5 -.-> F2["builds: .opencode/skills/{id}/SKILL.md"]
    S5 -.-> F3["builds: .opencode/skills/{id}/README.md"]
    S5 --> S6["6: Dependency graph - fill the README callers graph"]
    S6 --> S7["7: Workflow graph - fill the README steps graph"]
    S7 --> S8["8: GREEN gate - rerun the rule tests"]
    S8 --> S9["9: Report - RED and GREEN evidence, per-rule results"]
```
