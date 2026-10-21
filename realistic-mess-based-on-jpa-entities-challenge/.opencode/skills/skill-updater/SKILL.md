---
name: Skill Updater
description: Update an existing OpenCode skill's responsibility, scope, out-of-scope, workflow, rules, or acceptance criteria. Use when an existing skill must be changed, extended, or corrected, never to create a new one.
---

# Skill Updater

Update one existing OpenCode skill from the inputs the user provides. Do not
invent inputs, and do not write files until every required input is known.

## Purpose

Change the specified parts of an existing skill while leaving everything else
exactly as it was. Updating is the mirror of creation: the skill and its tests
already exist, and both must stay consistent after the edit.

## Scope

**In scope**

- Updating any part of an existing skill: responsibility, scope, out-of-scope,
  workflow, rules or acceptance criteria.
- Batching several categories in one run.
- Re-running the rule tests after the edit and reporting the verdict.

**Out of scope**

- Creating a skill. If the skill does not exist, do not create it; stop and
  report the missing path.
- Editing `tests/` files directly. Rules and acceptance-criteria changes are
  handed to `skill-testing` by the orchestrating agent.
- Running the skill itself or executing agent-evaluation prompts.
- Committing or pushing changes.

## Required inputs

Collect these three items:

1. **Target skill** — the kebab-case ID of the existing skill to update.
2. **Categories** — one or more of: responsibility, scope, out-of-scope, workflow, rules, acceptance criteria.
3. **New content** — the exact replacement content for each requested category.

If any item is missing or ambiguous, ask the user for all missing items in a
**single batched question** (one `question` call with one question per missing
item). Do not write or update any file until all three are present.

## Workflow

1. **Verify the target** — resolve `<id>/SKILL.md` under the discovered
   skills roots. If it does not exist, stop and report: updating never
   creates a skill.
2. **Intake** — collect the three inputs as described above. Stop if any is
   missing.
3. **Read** — load the target `SKILL.md` and, when rules or acceptance
   criteria are requested, its `tests/rules.json`.
4. **Edit** — change only the sections named by the requested categories;
   every other section stays untouched. Keep the description's `when to use`
   guidance accurate after the edit.
5. **Hand off tests** — when rules or acceptance criteria changed, give the
   new rule text to `skill-testing` so the spec and the `## Rules` table stay
   covered bidirectionally. The updater does not edit `tests/` itself.
6. **GREEN gate** — the orchestrating agent reruns
   `python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py <skill-dir>`.
   The run must be GREEN before success is reported. Fix the skill, never the
   assertions; weakening a rule requires explicit user approval.
7. **Report** — show a before/after diff of every edited section and the
   GREEN test output.

## Rules

| ID  | Rule                                                                       | Check            |
| --- | -------------------------------------------------------------------------- | ---------------- |
| R1  | A missing target skill is refused; the updater never creates it            | static           |
| R2  | The target skill is resolved as a lowercase kebab-case ID                  | static           |
| R3  | Only the six listed categories are accepted as update scope                | static           |
| R4  | Missing inputs are requested in one batched question before any edit       | static           |
| R5  | Only the requested categories are edited; everything else stays untouched  | static           |
| R6  | Rules changes are handed to skill-testing for bidirectional coverage       | static           |
| R7  | The rule-test runner must be GREEN before success is reported              | static           |
| R8  | The description still states when to use the skill                         | static           |
| R9  | The report includes before/after evidence and the test results             | static           |
| R10 | An incomplete update request is answered by asking, never by writing files | agent-evaluation |

## Acceptance criteria

- [ ] A request for a skill that does not exist is refused with the missing
      path reported and no file written.
- [ ] No file is written while a required input is missing; the ask is one
      batched question.
- [ ] Only the sections for the requested categories change; a diff shows all
      other content untouched.
- [ ] When rules or acceptance criteria change, `tests/rules.json` and the
      `## Rules` table still cover each other bidirectionally.
- [ ] The rule-test run is GREEN before success is reported.
- [ ] The description still says when to use the skill.
- [ ] The report shows a before/after diff and the GREEN output.
