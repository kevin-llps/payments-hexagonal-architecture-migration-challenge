# Skill Testing

Turns a skill's acceptance criteria into atomic, stable rule IDs and executable
checks. `tests/rules.json` is the authoritative specification, and the
zero-dependency runner reports one PASS/FAIL line per static rule, followed by
every agent-evaluation prompt.

## When to use

Use when a skill must be tested, when a skill is written test-first, or when
verifying that an existing skill satisfies its rules.

## How to run

```sh
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py .opencode/skills/<id>
```

Exit codes: `0` GREEN, `1` RED, `2` bad spec.

## Dependency graph

```mermaid
graph TD
    A1["1: skill-forge agent - loads this skill for the RED gate"] --> S["skill-testing"]
    A2["2: skill-updater agent - loads this skill for the tests handoff"] --> S
    A3["3: skill-creator skill - delegates rule decomposition and the runner"] --> S
```

## Workflow graph

```mermaid
graph TD
    S1["1: Read the skill's responsibility, instructions, and acceptance criteria"] --> S2["2: Split the criteria into atomic rules with stable IDs"]
    S2 --> S3["3: Write tests/rules.json before SKILL.md exists"]
    S3 -.-> F1["builds: tests/rules.json"]
    S3 --> S4["4: Choose assertions and agent-evaluation fixtures"]
    S4 -.-> F2["builds: tests/agent-evaluations/<ruleId>.md"]
    S4 --> S5["5: Run the runner - RED while SKILL.md is missing"]
    S5 --> S6["6: Hand the rule IDs to the authoring skill, then rerun - GREEN"]
    S6 --> S7["7: Report the rule table, PASS/FAIL per rule, and evaluation results"]
```
