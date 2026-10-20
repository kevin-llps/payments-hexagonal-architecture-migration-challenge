---
name: Skill Creator
description: Create a new OpenCode skill from user-provided inputs (responsibility, in-scope, out-of-scope, instructions, acceptance criteria). Use when the user asks to create, define, or scaffold a skill.
---

# Skill Creator

Create one OpenCode skill from the inputs the user provides. Do not invent
inputs, and do not write files until every required input is known.

## Required inputs

Collect these five items:

1. **Responsibility** — what the skill is for, in one or two sentences.
2. **In-scope** — what the skill must cover.
3. **Out-of-scope** — what it must not do, including things a reader might
   wrongly assume it handles.
4. **Instructions** — the ordered steps needed to complete the purpose,
   including commands, tools, and any supporting files.
5. **Acceptance criteria** — observable, checkable conditions proving the
   skill executed successfully.

If any item is missing or ambiguous, ask the user for all missing items in a
**single batched question** (one `question` call with one question per missing
item). Do not write, scaffold, or update any file until all five are present.
Do not fill gaps with assumptions or placeholders.

## Resolve the target

1. Derive a stable ID from the skill name: lowercase kebab-case matching
   `^[a-z0-9]+(-[a-z0-9]+)*$`.
2. Determine the target source. If the user did not specify, ask whether the
   skill is project-local or global:
   - Project: `<project>/.opencode/skills/<id>/SKILL.md`
   - Global: `~/.config/opencode/skills/<id>/SKILL.md`
3. Check for an existing skill with the same ID in any discovered source
   (`<source>/<id>/SKILL.md`, `<source>/<id>.md`). This skill is **create-only**:
   if the ID is taken, stop, report the existing path, and ask for a different
   ID. Never overwrite or edit an existing skill.
4. Confirm the final path before writing when the location is not obvious.

## Author the skill

Create directories as needed and write exactly one `<id>/SKILL.md`.
Supporting `references/`, `scripts/`, or `templates/` files are optional; add
them only when the instructions actually require them, and reference them by
paths relative to the skill directory.

Use this structure:

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

## Acceptance criteria

- [ ] <Input 5 items, each observable and checkable.>
```

Rules for the frontmatter:

- `name` is a human-readable display label, not the ID.
- `description` must state **when** to use the skill, because that is the only
  text shown to the model when deciding whether to load it. Omit it and the
  skill is not advertised.
- Keep the directory name equal to the ID; the ID comes from the path, not
  from `name`.

Map each input to exactly one section. Put scope items only under their own
heading. Never move out-of-scope items into the workflow as optional steps.

## Validate before reporting

Check every item and fix the file if any fails:

- `SKILL.md` sits directly in `<id>/`, so the path-derived ID equals the
  intended ID (case-sensitive).
- Frontmatter is valid YAML with both `name` and `description`.
- The description says when to use the skill, not only what it does.
- All five inputs trace to a section, and no input is missing or paraphrased
  into something broader.
- Out-of-scope items appear nowhere in the workflow as permitted actions.
- Every relative path referenced in the body resolves from the skill directory.
- No other discovered source defines the same ID.

## Report

Return:

- Final path and resulting skill ID.
- How to invoke it: `@<id>` in a prompt, or the `skill` tool with that ID.
- Validation checklist results, one line per item.
- The original acceptance criteria and whether the created skill can satisfy
  them as written.

If the user supplied no acceptance criteria for a new skill, report that the
skill is unverified rather than declaring it successful.
