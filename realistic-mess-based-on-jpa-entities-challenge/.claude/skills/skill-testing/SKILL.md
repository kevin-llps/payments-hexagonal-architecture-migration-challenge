---
name: Skill Testing
description: Decompose a skill's acceptance criteria into atomic rules, write executable tests for them, and iterate rule by rule from RED to GREEN. Use when a skill must be tested, when a skill is written test-first, or when verifying that an existing skill satisfies its rules.
---

# Skill Testing

## Invocation Announcement

Before any other output or tool call, display exactly this line as the skill's first output:

`▶ Skill invoked: Skill Testing — turn skill acceptance criteria into rule tests, iterated RED to GREEN`

Display it on every invocation, whether triggered by the user, an agent, or a skill. When delegating to a subagent or loading a skill, do not print its announcement on its behalf; it prints its own. The announcement never replaces or delays a mandatory approval gate.

Turn a skill's acceptance criteria into atomic, stable rule IDs and executable
checks. Tests are the specification: they are written before the skill they
describe, and the runner reports one PASS/FAIL line per static rule. The whole
spec is written first, but the implementation advances one rule at a time:
each rule is RED under `--rule <id>` before it is implemented and GREEN after.

## Purpose

Provide the rule schema, test fixtures, runner, and verdict used by
`skill-creator` and the `skill-forge` agent. It is also usable standalone to
regression-test any existing skill.

## Scope

**In scope**

- Decomposing acceptance criteria into atomic rules with stable IDs (`R1`,
  `R2`, ...).
- Writing `tests/rules.json` and agent-evaluation fixtures beside the skill.
- Running static checks and returning a deterministic RED/GREEN verdict.
- Running one selected rule at a time so each iteration has its own evidence.
- Listing every agent-evaluation rule, with its prompt, after the static run.

**Out of scope**

- Authoring or editing `SKILL.md`. The skill under test is read-only here.
- Judging agent-evaluation results automatically; the runner prints the prompt
  and an agent judges the answer.
- Installing tools or adding dependencies; the runner is zero-dependency Python.
- Publishing, committing, or moving skills.

## Workflow

1. Read the skill's responsibility, instructions, and acceptance criteria.
2. Split the acceptance criteria into atomic rules: one rule, one observable
   behavior, one stable ID. Each rule declares `check: "static"` or
   `check: "agent-evaluation"`.
3. Write `tests/rules.json` inside the skill directory. It is the authoritative
   spec and must exist before `SKILL.md`.
4. Choose assertions from `references/rules-schema.md`. Static rules need
   at least one assertion; agent-evaluation rules need
   `tests/agent-evaluations/<id>.md` with `Prompt` and `Expected` sections.
5. Iterate **one rule at a time**, in spec order, until every rule is GREEN.
   Hand each rule to the authoring skill; this skill never edits `SKILL.md`
   itself.
   1. Run that rule with
      `python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py <skill-dir> --rule <id>`.
   2. A static rule is RED while it prints `FAIL`; the authoring skill must
      implement only that rule in `SKILL.md`, then rerun the same command until
      the rule prints `PASS` and exits `0`.
   3. Agent-evaluation rules run **at their position in the loop**: the runner
      prints that rule's `Prompt` and `Expected`; execute the prompt against
      the current `SKILL.md` and compare the answer to `Expected`. A mismatch
      is RED; have the authoring skill add the missing guidance, rerun the
      same prompt, and record the PASS before continuing.
   4. Only then move to the next rule.
6. Run the full suite with no `--rule`. Every static rule must be GREEN and
   coverage between the spec and the `## Rules` table stays bidirectional.
   Agent evaluations are listed after the static run, never hidden behind a
   flag, and a missing fixture fails the run. Rerun every prompt once more if
   `SKILL.md` changed after its iteration.
7. Report the rule table, PASS/FAIL per rule, and the result of every
   agent-evaluation prompt.

## Rules

| ID  | Rule                                                           | Check            |
| --- | -------------------------------------------------------------- | ---------------- |
| R1  | Tests exist and are authoritative before SKILL.md              | static           |
| R2  | Each rule has a stable ID and an explicit check type           | static           |
| R3  | Static rules carry at least one executable assertion           | static           |
| R4  | Spec and Rules table cover each other bidirectionally          | static           |
| R5  | Agent evaluations are listed after the static run              | static           |
| R6  | The runner never edits the skill under test                    | static           |
| R7  | The verdict is one PASS/FAIL line per static rule              | static           |
| R8  | The runner is zero-dependency Python                           | static           |
| R9  | A skill is discoverable and its description says when to use it | static           |
| R10 | Compound acceptance criteria are split into separate atomic rules | agent-evaluation |
| R11 | Every script has a matching test module under scripts/tests    | static           |
| R12 | TDD goes one rule at a time: RED, implement, GREEN, next rule  | static           |
| R13 | The runner executes one selected rule via --rule and reports it | static          |
| R14 | Agent-evaluation rules run at their position in the loop       | static           |
| R15 | The first output line announces the skill invocation           | static           |

## Commands

```sh
# Static results first, then the agent-evaluation prompts: 0 = GREEN, 1 = RED, 2 = bad spec
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py .opencode/skills/<id>

# The skill argument may also be a bare ID, resolved from the cwd then the skills root
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py <id>

# One rule at a time: prints "Selected: R1 (1 of N)" and reports only that rule
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py <skill-dir> --rule R1

# `--rule` is repeatable (`--rule R1 --rule R2` or `--rule=R1`); unknown IDs exit `2`
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py <skill-dir> --rule R1 --rule R2

# Edit guard for a skill-forge run
python3 .opencode/skills/skill-testing/scripts/enforce-new-skill-scope.py snapshot
python3 .opencode/skills/skill-testing/scripts/enforce-new-skill-scope.py check --skill <id>

# Self-tests for both scripts (stdlib unittest, no network or Docker)
python3 -m unittest discover -s .opencode/skills/skill-testing/scripts/tests -v
```

## Acceptance criteria

- [ ] A spec without `SKILL.md` exits `1` and fails every static rule.
- [ ] A matching skill exits `0` with one PASS line per static rule.
- [ ] A rule ID missing on either side is reported as a coverage failure.
- [ ] A malformed spec exits `2` without running any assertion.
- [ ] Agent evaluations are printed after the static results with Prompt and Expected.
- [ ] A missing agent-evaluation fixture exits `1` instead of being skipped.
- [ ] Every script under `scripts/` has a matching `scripts/tests/test_<name>.py`.
- [ ] Each static rule is RED under `--rule <id>` before its implementation and GREEN after it.
- [ ] `--rule <id>` runs only the selected rule, prints `Selected:`, and exits `2` on an unknown ID.
- [ ] Each agent-evaluation prompt is executed at its rule's position, before the next rule starts.
- [ ] The first output line is `▶ Skill invoked: Skill Testing — turn skill acceptance criteria into rule tests, iterated RED to GREEN`.
