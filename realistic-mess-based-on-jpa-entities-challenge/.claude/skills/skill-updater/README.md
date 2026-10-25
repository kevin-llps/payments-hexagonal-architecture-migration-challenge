# Skill Updater

Updates exactly one existing OpenCode skill from explicit inputs — target ID,
categories, and new content — then proves the edit with the skill's rule tests.
It never creates a skill: a missing target is reported, not scaffolded.

## When to use

Use when an existing skill must be changed, extended, or corrected, never to
create a new one.

## How to run

Ask the `skill-updater` agent to update a skill, naming the target skill ID, the
categories (responsibility, scope, out-of-scope, workflow, rules, acceptance
criteria), and the exact new content for each.

## Dependency graph

```mermaid
graph TD
    A1["1: skill-updater agent - loads this skill for every update run"] --> S["skill-updater"]
```

## Workflow graph

```mermaid
graph TD
    S1["1: Verify the target - the skill must already exist"] --> S2["2: Intake - target ID, categories, new content"]
    S2 --> S3["3: Read the target SKILL.md and tests/rules.json when rules change"]
    S3 --> S4["4: Edit - change only the requested sections"]
    S4 --> S5["5: Hand off tests - skill-testing updates the spec"]
    S5 -.-> F1["builds: tests/rules.json"]
    S5 --> S6["6: GREEN gate - rerun the rule tests"]
    S6 --> S7["7: Report - before/after diff, GREEN output, change set"]
```
