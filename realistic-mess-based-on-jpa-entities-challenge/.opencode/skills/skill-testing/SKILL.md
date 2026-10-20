---
name: Skill Testing
description: Decompose a skill's acceptance criteria into atomic rules, write executable tests for them, and run them to a RED/GREEN verdict. Use when a skill must be tested, when a skill is written test-first, or when verifying that an existing skill satisfies its rules.
---

# Skill Testing

Turn a skill's acceptance criteria into atomic, stable rule IDs and executable
checks. Tests are the specification: they are written before the skill they
describe, and the runner reports one PASS/FAIL line per static rule.

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
5. Run `python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py <skill-dir>`.
   With no `SKILL.md` the run is RED: every static rule fails and the exit code
   is `1`.
6. Hand the rule IDs to the authoring skill. After `SKILL.md` exists, rerun the
   same command. The run is GREEN only when every static rule passes and
   coverage between the spec and the `## Rules` table is bidirectional.
   Agent evaluations are listed after the static run, never hidden behind a
   flag, and a missing fixture fails the run.
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

## Commands

```sh
# Static results first, then the agent-evaluation prompts: 0 = GREEN, 1 = RED, 2 = bad spec
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py .opencode/skills/<id>

# The skill argument may also be a bare ID, resolved from the cwd then the skills root
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py <id>

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
