---
description: Create a new skill test-first, one rule at a time (RED, implement, GREEN), without modifying existing skills
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

4. **Per-rule loop.** Return to `skill-creator` and iterate the spec in order,
   one rule at a time. Do not start the next rule before the current one is
   GREEN.
   - **Static rule.** Run
     `python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py .opencode/skills/<id> --rule <id>`.
     The first run before `SKILL.md` exists is the RED. Write only the content
     that rule needs, keeping the `## Rules` table listing exactly the spec
     IDs, then rerun until the rule prints `PASS` and the command exits `0`.
   - **Agent-evaluation rule.** Run the same command with `--rule <id>`. It
     prints that rule's `Prompt` and `Expected`. Launch a subagent with the
     current skill loaded, execute the prompt, and compare the answer. A
     mismatch is the RED; add the missing guidance to `SKILL.md`, rerun the
     prompt, and record the PASS before continuing.

5. **Full gate.** Run
   `python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py .opencode/skills/<id>`
   with no `--rule`. Every static rule must pass. Fix the skill, never the
   assertions; if a rule is genuinely wrong, ask the user before changing
   `tests/rules.json`. The runner also prints an `Agent evaluations` section
   after the static results.

6. **Agent evaluations.** If `SKILL.md` changed after a rule's evaluation,
   rerun that prompt and record the final PASS/FAIL per rule. A failing
   evaluation is reported, never fixed by weakening the fixture. Never edit an
   evaluation result into a passing one.

7. **Guard.** Run
   `python3 .opencode/skills/skill-testing/scripts/enforce-new-skill-scope.py check --skill <id>`.
   It must report `CLEAN`. A violation means an existing skill was modified or
   a file was written outside `.opencode/skills/<id>/`.

8. **Report.** Show the final path, the per-rule RED then GREEN evidence, the
   full-run output, the agent-evaluation results, and the guard result.

## Hard limits

- One new skill per run. Do not create or edit a second skill.
- Never overwrite, rename, or delete an existing skill, test, or reference
  file. Existing skills are read-only regardless of what the user asks.
- Never claim success without per-rule RED then GREEN evidence and a final
  full-run GREEN.
- If a gate cannot be reached, stop and report the blocker instead of writing
  around it.
