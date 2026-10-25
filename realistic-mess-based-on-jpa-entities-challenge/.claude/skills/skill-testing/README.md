# Skill Testing

Turns a skill's acceptance criteria into atomic, stable rule IDs and executable
checks. `tests/rules.json` is the authoritative specification, and the
zero-dependency runner reports one PASS/FAIL line per static rule, followed by
every agent-evaluation prompt. The spec is written first, then implemented one
rule at a time: each rule gets its own RED, implementation, and GREEN.

## When to use

Use when a skill must be tested, when a skill is written test-first, or when
verifying that an existing skill satisfies its rules.

## How to run

```sh
# One rule of the loop
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py .opencode/skills/<id> --rule R1

# Release gate: every static rule, then the agent-evaluation prompts
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py .opencode/skills/<id>
```

`--rule` is repeatable (`--rule R1 --rule R2` or `--rule=R1`). Exit codes:
`0` GREEN, `1` RED, `2` bad spec or unknown rule ID.

## Dependency graph

```mermaid
graph TD
    A1["1: skill-forge agent - loads this skill for the RED gate"] --> S["skill-testing"]
    A2["2: skill-updater agent - loads this skill for the tests handoff"] --> S
```

## Workflow graph

```mermaid
graph TD
    S1["1: Read the skill's responsibility, instructions, and acceptance criteria"] --> S2["2: Split the criteria into atomic rules with stable IDs"]
    S2 --> S3["3: Write tests/rules.json before SKILL.md exists"]
    S3 -.-> F1["builds: tests/rules.json"]
    S3 --> S4["4: Choose assertions and agent-evaluation fixtures"]
    S4 -.-> F2["builds: tests/agent-evaluations/<ruleId>.md"]
    S4 --> S5["5: Take the next rule in spec order"]
    S5 --> S6{"6: Static or agent-evaluation?"}
    S6 -->|static| S7["7: Run --rule id - RED while it fails"]
    S7 --> S8["8: Implement only that rule in SKILL.md"]
    S8 --> S9["9: Rerun --rule id - GREEN"]
    S6 -->|agent-evaluation| S10["10: Execute Prompt, compare to Expected - RED on mismatch"]
    S10 --> S11["11: Implement the missing guidance"]
    S11 --> S12["12: Rerun the prompt - PASS"]
    S9 --> S13{"13: Rules left?"}
    S12 --> S13
    S13 -->|yes| S5
    S13 -->|no| S14["14: Full run - all static GREEN, evaluations listed"]
    S14 --> S15["15: Report the rule table, PASS/FAIL per rule, and evaluation results"]
```
