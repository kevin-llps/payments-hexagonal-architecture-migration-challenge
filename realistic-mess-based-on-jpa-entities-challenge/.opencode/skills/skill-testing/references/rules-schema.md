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
    └── agent-evaluations/
        └── <ruleId>.md      # only for check: "agent-evaluation"
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
      "rule": "Behavioral rule checked by an agent",
      "check": "agent-evaluation"
    }
  ]
}
```

| Field | Rule |
| ----- | ---- |
| `skill` | Must equal the skill directory name (kebab-case). |
| `id` | `R` followed by digits; unique within the spec. |
| `rule` | One-line atomic behavior. |
| `check` | `"static"` (deterministic) or `"agent-evaluation"` (agent-run). |
| `assertions` | Required for static rules, at least one entry. |

Agent-evaluation rules need `tests/agent-evaluations/<id>.md`. A missing or
malformed fixture is a failure, not a skip.

`SKILL.md` must contain a `## Rules` table whose first column lists exactly the
spec IDs. Coverage is checked in both directions, so a rule cannot exist only
in the table or only in the spec.

## Selected-rule runs

The spec is always written in full first. The implementation then advances one
rule at a time, and each iteration is observed with `--rule`:

```sh
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py <skill-dir> --rule R1
```

`--rule` is repeatable and also accepts `--rule=<id>`. The run keeps spec order,
evaluates only the selected rules, and prints a `Selected: R1 (1 of N)` line.
The whole spec is still validated, `ruleTableCoverage` still compares against
every spec ID, and an unknown ID exits `2` before any assertion runs. A
selected agent-evaluation rule is listed alone so its prompt can be executed at
that point in the loop.

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
| `scriptTestCoverage` | — | Every `scripts/*.py` has a matching `scripts/tests/test_<name>.py`. |

A `section` value must match a heading text exactly (case-insensitive), without
the `#` markers. Section scanning stops at the next heading of the same or
higher level and ignores headings inside fenced code blocks.

## Script tests

Every `scripts/*.py` in a skill must have a matching stdlib `unittest` module at
`scripts/tests/test_<name>.py` (hyphens become underscores). The
`scriptTestCoverage` assertion enforces this, and it runs itself for
`skill-testing`:

```sh
python3 -m unittest discover -s .opencode/skills/skill-testing/scripts/tests -v
python3 .opencode/skills/skill-testing/scripts/run-skill-rule-tests.py skill-testing
```

Tests use only the standard library: no network, Docker, or extra packages.

## Agent-evaluation fixtures

Only used with `check: "agent-evaluation"`. Create
`tests/agent-evaluations/<ruleId>.md`:

```markdown
# R2 — Missing input

## Prompt

Create a skill called weather without telling it what to do.

## Expected

The agent asks for the missing responsibility before writing any file.
```

The runner does not call a model. It validates that the fixture has `Prompt`
and `Expected` sections, then lists every agent-evaluation rule **after** the
static results, with the prompt an agent must execute. The orchestrating agent
runs the prompt and reports the observed behavior. In the per-rule loop, that
evaluation happens at the rule's position: a mismatch with `Expected` is the
RED, and a rerun after the fix is the GREEN. There is no flag: these checks are
always listed, and an unusable fixture fails the run.

## Output order

1. `Rules:` count, plus `Selected:` when `--rule` is used.
2. Static results, one `PASS`/`FAIL` line per static rule.
3. `Static:` summary.
4. `Agent evaluations (not executed by this script — run with an agent):`
   followed by one `AGENT-EVAL` block per rule with `Prompt` and `Expected`
   (or `FAIL` with the fixture problem).
5. `Agent evaluations to run: N`.
6. `Result:` — `GREEN`, `GREEN (static) — N agent evaluation(s) pending`, or
   `RED`.

## Exit codes

| Code | Meaning |
| ---- | ------- |
| `0` | Every static rule passed and every agent-evaluation fixture is usable. |
| `1` | At least one static rule failed, no spec/`SKILL.md` yet, or an agent-evaluation fixture is missing/malformed. |
| `2` | Usage error, malformed spec, or unknown rule ID. |

## Guard

`run-skill-rule-tests.py` checks a skill against its `tests/rules.json` spec.
`enforce-new-skill-scope.py snapshot` records hashes of every file under the
skills root. `enforce-new-skill-scope.py check --skill <id>` fails on any
modification or deletion of a snapshotted file, and on any new file outside
`<id>/`. It uses a single snapshot at
`$TMPDIR/opencode/skill-forge-baseline.json`; take a new snapshot at the start
of each run.
