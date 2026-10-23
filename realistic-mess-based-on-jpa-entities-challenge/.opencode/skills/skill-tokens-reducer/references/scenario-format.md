# Scenario format

One JSON file per scenario, stored at
`.opencode/skills/skill-tokens-reducer/scenarios/<target>/<id>.json`.

```json
{
  "id": "nominal",
  "reason": "Create a skill from a complete five-input request",
  "prompt": "Load the skill-creator skill and create a skill named demo from these five inputs: ...",
  "model": "opencode/big-pickle#default",
  "agent": "build",
  "assertions": {
    "files": [".opencode/skills/demo/SKILL.md"],
    "contains": [
      { "path": ".opencode/skills/demo/SKILL.md", "text": "## Workflow" }
    ],
    "not_contains": [
      { "path": ".opencode/skills/demo/SKILL.md", "text": "TODO" }
    ],
    "final_text_contains": ["created"],
    "commands": [["python3", ".opencode/skills/skill-testing/scripts/run-skill-rule-tests.py", ".opencode/skills/demo"]],
    "artifacts": [".opencode/skills/demo/SKILL.md"],
    "rule_tests": true
  }
}
```

## Field rules

- `id` defaults to the file stem. Keep it stable; it keys the run directories.
- `prompt` must tell the agent to load the target skill. The runner does not
  inject the skill automatically.
- `reason` is copied verbatim into `TOKENS_HISTORY.md` as "why invoked".
- `model` and `agent` are optional. A CLI `--model` / `--agent` overrides them;
  every phase must use the same values.
- `assertions.files` lists paths that must exist after the run.
- `assertions.contains` / `not_contains` check exact text in a path.
- `assertions.final_text_contains` checks the assistant's final text.
- `assertions.commands` runs argv lists (or a shell string) in the sandbox; exit
  0 is required.
- `assertions.artifacts` hashes result files. The hashes must be identical in
  every run of every phase, otherwise the candidate is rejected.
- `rule_tests` defaults to true when the target has `tests/rules.json`. The
  target's full static rule suite must stay GREEN; its `tests/` tree is hashed
  across phases and any byte change is a rejection.

## Writing the three scenarios

Use one nominal request, one edge case, and one ambiguous or under-specified
request. Assert observable results, not the agent's exact wording, except where
the wording is the product. Keep scenario files byte-identical between the
baseline and candidate phases.

Each run is a copy of the project without `.git`, `target`, and `node_modules`.
Prompts must not depend on git history or prebuilt artifacts. The copy starts
from the project root that contains `.opencode/skills`.
