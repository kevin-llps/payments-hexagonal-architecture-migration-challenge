---
name: Skill Creator
description: Create a new OpenCode skill from user-provided inputs (responsibility, in-scope, out-of-scope, instructions, acceptance criteria), test-first with rules and failing tests before SKILL.md. Use when the user asks to create, define, or scaffold a skill.
---

# Skill Creator

Create one OpenCode skill from the inputs the user provides. Do not invent
inputs, and do not write files until every required input is known.

## Purpose

Turn the five user inputs into a discoverable, test-first OpenCode skill. The
atomic rules and their failing tests come first; `SKILL.md` is written to make
those tests pass.

## Scope

**In scope**

- Collecting and validating the five required inputs.
- Resolving a kebab-case ID and a project-local target path.
- Delegating rule decomposition and test authoring to `skill-testing`.
- Authoring `SKILL.md` only after the test run is RED.
- Reporting RED and GREEN evidence.

**Out of scope**

- Overwriting, updating, or deleting existing skills. Creation is create-only.
- Writing skills outside `.opencode/skills/` unless the user explicitly asks.
- Committing or pushing changes.
- Executing agent-evaluation prompts; the orchestrating agent runs them after
  the static gate.

## Required inputs

Collect these five items:

1. **Responsibility** — what the skill is for, in one or two sentences.
2. **In-scope** — what the skill must cover.
3. **Out-of-scope** — what it must not do, including things a reader might
   wrongly assume it handles.
4. **Instructions** — the ordered steps needed to complete the purpose,
   including commands, tools, and any supporting files.
5. **Acceptance criteria** — observable, checkable conditions proving the
   skill executed successfully. Each criterion becomes at least one atomic rule.

If any item is missing or ambiguous, ask the user for all missing items in a
**single batched question** (one `question` call with one question per missing
item). Do not write, scaffold, or update any file until all five are present.
Do not invent inputs or fill gaps with placeholders.

## Workflow

1. **Intake** — collect the five inputs as described above. Stop if any is
   missing.
2. **Resolve the target** — derive a lowercase kebab-case ID matching
   `^[a-z0-9]+(-[a-z0-9]+)*$`. If the user did not specify scope, ask whether
   the skill is project-local or global. Project: `.opencode/skills/<id>/`.
3. **Refuse collisions** — check every discovered source for
   `<id>/SKILL.md` and `<id>.md`. If the ID already exists, stop, report the
   existing path, and ask for a different ID. Never overwrite or edit an
   existing skill.
4. **Test-first** — hand the five inputs to `skill-testing`. It derives atomic
   rule IDs and writes `.opencode/skills/<id>/tests/rules.json`. Run
   `python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py .opencode/skills/<id>`.
   The run must fail before `SKILL.md` exists: that failure is the RED gate. Do
   not author `SKILL.md` until RED is observed.
5. **Author** — write exactly one `<id>/SKILL.md` using the template below. Its
   `## Rules` table must list exactly the rule IDs from `tests/rules.json`.
   Then write `<id>/README.md` next to it using the README template below; the
   README is mandatory, not optional. Supporting `references/`, `scripts/`, or
   `templates/` files are optional and must be referenced by paths relative to
   the skill directory.
6. **Dependency graph** — fill the `## Dependency graph` section of
   `<id>/README.md` with one indexed caller sentence per agent or skill that
   calls this skill (`1: <caller>`, `2: <caller>`, ...), one arrow per caller
   pointing to this skill, following the graph rules below.
7. **Workflow graph** — fill the `## Workflow graph` section of
   `<id>/README.md` with one indexed step sentence per step of this skill's
   own workflow (`1: <step>`, `2: <step>`, ...) in execution order, plus the
   files each step builds, following the graph rules below.
8. **GREEN gate** — rerun the same command. It must pass before reporting
   success. Fix the skill, not the tests; weakening a rule requires explicit
   user approval.
9. **Report** — list the final path, the skill ID, RED and GREEN evidence, the
   per-rule results, and the agent-evaluation results.

### README template

```markdown
# <Display Name>

<One-paragraph purpose, from input 1.>

## When to use

<When to use this skill, mirroring the description input.>

## How to run

<How the user invokes the skill: the exact prompt phrase or command.>

## Dependency graph

<MERMAID graph TD: one node for this skill plus one node per agent or skill
that calls it. Every caller node label starts with its 1-based index followed
by ": " - e.g. "1: <first caller sentence>", "2: <second caller sentence>".
One arrow per caller, pointing from the caller to this skill.>

## Workflow graph

<MERMAID graph TD: one node per step of this skill's own workflow, every node
label starting with its 1-based index followed by ": " - e.g.
"1: <first step sentence>", "2: <second step>", arrows in execution order.
Each file a step builds hangs off its step with a "builds: <path>" label.>
```

README template rules:

- The README documents the skill's purpose, when to use it, and how to run it.
- The README stays shorter than `SKILL.md`; it documents, it does not duplicate.
- Node labels in both graphs begin with the entry's 1-based index followed by
  ": ", e.g. `1: <sentence>`; indexes follow the order in which the arrows
  point.
- The dependency graph shows only agents or skills that call this skill plus
  the skill itself: one arrow per caller, pointing from the caller to the
  skill. No outgoing dependencies, no unrelated nodes, and no invented
  callers; if nothing calls the skill yet, one sentence says so instead.
- The workflow graph shows only steps executed by this skill, in execution
  order, with the files each step builds. Steps executed by an agent that
  invokes this skill are excluded.

## Rules

| ID  | Rule                                                                | Check  |
| --- | ------------------------------------------------------------------- | ------ |
| R1  | All five inputs are required before any file is written              | static |
| R2  | Missing inputs are asked in a single batched question                | static |
| R3  | Creation is create-only; an existing ID is refused                   | static |
| R4  | The skill ID is lowercase kebab-case                                 | static |
| R5  | Every generated skill carries a mandatory Rules table                | static |
| R6  | Rules and a failing test run exist before SKILL.md is authored       | static |
| R7  | Static tests must pass before success is reported                    | static |
| R8  | The report includes RED and GREEN evidence and agent-evaluation results  | static |
| R9  | The description states when to use the skill                         | static |
| R10 | An incomplete request is answered by asking, never by writing files | agent-evaluation |
| R11 | Every created skill ships a README.md documentation file            | static |
| R12 | The README documents the skill's purpose, when to use it, and how to run it | static |
| R13 | The README dependency graph shows only this skill's callers, each label indexed | static |
| R14 | The README workflow graph shows only this skill's own steps, each label indexed   | static |

## Template

```markdown
---
name: <Display Name>
description: <When to use this skill, in one sentence>
---

# <Display Name>

## Purpose

<Responsibility, from input 1.>

## Scope

**In scope**
- <Input 2 items.>

**Out of scope**
- <Input 3 items.>

## Workflow

1. <Input 4 steps, in execution order, with concrete commands or tools.>

## Rules

| ID  | Rule | Check  |
| --- | ---- | ------ |
| R1  | <Atomic behavior> | static |
| R2  | <Atomic behavior> | eval   |

## Acceptance criteria

- [ ] <Input 5 items, each observable and checkable.>
```

Template rules:

- `name` is a human-readable display label, not the ID.
- `description` must state when to use the skill; it is the only text shown to
  the model when deciding whether to load it. Omit it and the skill is not
  advertised.
- The directory name is the ID; it must stay equal to the ID.
- Map each input to exactly one section. Put scope items only under their own
  heading and never repeat an out-of-scope item as an optional workflow step.

## Acceptance criteria

- [ ] No file is written while an input is missing.
- [ ] An existing skill ID stops the run with the existing path reported.
- [ ] RED is observed before `SKILL.md` is created.
- [ ] GREEN is observed before success is reported.
- [ ] The generated `SKILL.md` passes `skill-testing` with exact rule coverage.
- [ ] `<id>/README.md` exists beside `<id>/SKILL.md` for every created skill.
- [ ] The README documents the skill's purpose, when to use it, and how to run
      it.
- [ ] The README's dependency graph shows only this skill's callers, each
      label indexed as `1: <caller>`.
- [ ] The README's workflow graph shows only this skill's own steps, each
      label indexed as `1: <step>`.
