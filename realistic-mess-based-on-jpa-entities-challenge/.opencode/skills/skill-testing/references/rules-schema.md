# Rules schema

`tests/rules.json` is the authoritative specification for a skill. It is
written before `SKILL.md` and lives inside the skill directory:

```text
.opencode/skills/<id>/
├── SKILL.md
├── references/
├── scripts/
└── tests/
    ├── rules.json
    └── evals/
        └── <ruleId>.md      # only for check: "eval"
```

## Shape

```json
{
  "skill": "example",
  "rules": [
    {
      "id": "R1",
      "rule": "Missing inputs are requested before any file is written",
      "check": "static",
      "assertions": [
        { "type": "contains", "value": "until all five are present" }
      ]
    },
    {
      "id": "R2",
      "rule": "Behavioral rule checked with an eval fixture",
      "check": "eval"
    }
  ]
}
```

| Field | Rule |
| ----- | ---- |
| `skill` | Must equal the skill directory name (kebab-case). |
| `id` | `R` followed by digits; unique within the spec. |
| `rule` | One-line atomic behavior. |
| `check` | `"static"` (deterministic) or `"eval"` (advisory). |
| `assertions` | Required for static rules, at least one entry. |

`SKILL.md` must contain a `## Rules` table whose first column lists exactly the
spec IDs. Coverage is checked in both directions, so a rule cannot exist only
in the table or only in the spec.

## Assertion types

| Type | Extra fields | Passes when |
| ---- | ------------ | ----------- |
| `frontmatterField` | `value`, optional `matches`, `flags` | The YAML frontmatter has the field non-empty, and the optional regex matches it. `flags` defaults to `i`. |
| `heading` | `value` | A heading with that text exists (fenced code blocks are ignored). |
| `contains` | `value`, optional `section` | The text occurs in the body, or within the named section. |
| `notContains` | `value`, optional `section` | The text does not occur. |
| `regex` | `value`, optional `flags`, `section` | The pattern matches the body or the named section. |
| `pathExists` | `value` | The path resolves from the skill directory. |
| `idMatchesDir` | — | The directory name matches `^[a-z0-9]+(-[a-z0-9]+)*$`. |
| `ruleTableCoverage` | — | Spec IDs and `## Rules` table IDs match exactly. |

A `section` value must match a heading text exactly (case-insensitive), without
the `#` markers. Section scanning stops at the next heading of the same or
higher level and ignores headings inside fenced code blocks.

## Eval fixtures

Only used with `check: "eval"`. Create `tests/evals/<ruleId>.md`:

```markdown
# R2 — Missing input

## Prompt

Create a skill called weather without telling it what to do.

## Expected

The agent asks for the missing responsibility before writing any file.
```

The runner does not call a model. It validates that the fixture has `Prompt`
and `Expected` sections and prints it as advisory output; the orchestrating
agent executes the fixture and reports the observed behavior.

## Exit codes

| Code | Meaning |
| ---- | ------- |
| `0` | Every static rule passed (GREEN). |
| `1` | At least one static rule failed, or no spec/`SKILL.md` yet (RED). |
| `2` | Usage error or malformed spec. |

## Guard

`run-skill-rule-tests.py` checks a skill against its `tests/rules.json` spec.
`enforce-new-skill-scope.py snapshot` records hashes of every file under the
skills root. `enforce-new-skill-scope.py check --skill <id>` fails on any
modification or deletion of a snapshotted file, and on any new file outside
`<id>/`. It uses a single snapshot at
`$TMPDIR/opencode/skill-forge-baseline.json`; take a new snapshot at the start
of each run.
