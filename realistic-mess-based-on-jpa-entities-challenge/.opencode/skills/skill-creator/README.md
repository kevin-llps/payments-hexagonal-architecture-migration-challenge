# Skill Creator

Turns five user-provided inputs — responsibility, in-scope, out-of-scope,
instructions and acceptance criteria — into a discoverable, test-first OpenCode
skill. The skill itself calls no skill, agent, or sub-agent, and performs no
rule decomposition, test authoring, or RED/GREEN gating.

## When to use

Use when the user asks to create, define, or scaffold a new skill.

## How to run

Ask for a new skill through the `skill-forge` agent — for example
"create a skill called weather" — and provide the five inputs when asked.

## Dependency graph

```mermaid
graph TD
    A1["1: skill-forge agent - calls skill-creator"] --> S["skill-creator"]
```

## Workflow graph

```mermaid
graph TD
    S1["1: Intake - collect the five inputs"] --> S2["2: Resolve the target - kebab-case ID"]
    S2 --> S3["3: Refuse collisions - creation is create-only"]
    S3 --> S4["4: Testing stays outside - this skill calls no skill, agent, or sub-agent"]
    S4 --> S5["5: Author - SKILL.md and README.md"]
    S5 -.-> F2["builds: .opencode/skills/{id}/SKILL.md"]
    S5 -.-> F3["builds: .opencode/skills/{id}/README.md"]
    S5 --> S6["6: Dependency graph - fill the README callers graph"]
    S6 --> S7["7: Workflow graph - fill the README steps graph"]
    S7 --> S8["8: Report - final path, skill ID, and created files"]
```
