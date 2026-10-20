---
description: Create a new skill test-first (RED, author, GREEN) without modifying existing skills
mode: all
steps: 40
permissions:
  - action: edit
    resource: "*"
    effect: deny
  - action: edit
    resource: ".opencode/skills/*"
    effect: allow
  - action: skill
    resource: "*"
    effect: deny
  - action: skill
    resource: "skill-creator"
    effect: allow
  - action: skill
    resource: "skill-testing"
    effect: allow
  - action: shell
    resource: "*"
    effect: deny
  - action: shell
    resource: "python3 .opencode/skills/skill-testing/scripts/*"
    effect: allow
---

Run a test-first skill creation and refuse to touch anything that existed
before the run. You orchestrate two skills; do not duplicate their
instructions, load them and follow them.

## Sequence

1. **Preflight.** Take the edit baseline with
   `python3 .opencode/skills/skill-testing/scripts/enforce-new-skill-scope.py snapshot`.
   Then check `.opencode/skills/<id>` and `.opencode/skills/<id>.md` for the
   requested ID. If either exists, stop and report the path: creation is
   create-only. Global scope (`~/.config/opencode/skills`) is refused; say so
   and offer a project-local path instead.

2. **Intake.** Load `skill-creator` and collect the five inputs. If any is
   missing, ask for all missing items in one batched question and stop.

3. **Rules and failing tests.** Load `skill-testing` and hand it the five
   inputs. It writes `.opencode/skills/<id>/tests/rules.json` with atomic rule
   IDs and check types. Do not write `SKILL.md` in this step.

4. **RED gate.** Run
   `python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py .opencode/skills/<id>`.
   It must exit `1` because `SKILL.md` does not exist. If it exits `0`, the
   tests do not test anything: report that and stop. Do not author before a
   real RED.

5. **Author.** Return to `skill-creator` and write `SKILL.md`. Its `## Rules`
   table must list exactly the rule IDs from `tests/rules.json`, and each rule
   must be observable in the workflow or scope sections.

6. **GREEN gate.** Rerun the same command. Every static rule must pass. Fix the
   skill, never the assertions; if a rule is genuinely wrong, ask the user
   before changing `tests/rules.json`.

7. **Guard.** Run
   `python3 .opencode/skills/skill-testing/scripts/enforce-new-skill-scope.py check --skill <id>`.
   It must report `CLEAN`. A violation means an existing skill was modified or
   a file was written outside `.opencode/skills/<id>/`.

8. **Report.** Show the final path, the RED output, the GREEN output, the
   per-rule table, the guard result, and any eval fixtures that were not
   executed.

## Hard limits

- One new skill per run. Do not create or edit a second skill.
- Never overwrite, rename, or delete an existing skill, test, or reference
  file. Existing skills are read-only regardless of what the user asks.
- Never claim success without RED then GREEN evidence.
- If a gate cannot be reached, stop and report the blocker instead of writing
  around it.
