# Token history format

`TOKENS_HISTORY.md` lives in the measured skill's directory:
`.opencode/skills/<target>/TOKENS_HISTORY.md`.

## Metric

```text
Total = Input + Cache read + Cache write + Output + Reasoning
```

- OpenCode stores `Input` as **non-cached** input tokens, so cache tokens are
  added exactly once.
- `Total` covers the root run session and every child session.
- `Date` is the root session's creation date (`YYYY-MM-DD`).
- `Cost` and duration are recorded in run JSON but are not used for acceptance.

## Template

```markdown
# Token history - <target>

## Runs

| Date | Session ID | Children | Phase | Scenario | Run | Session title | Why invoked | Model / agent | Input | Output | Reasoning | Cache read | Cache write | Total | Result |
| --- | --- | ---: | --- | --- | ---: | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 2026-10-23 | `ses_...` | 0 | baseline | nominal | 1 | tokens:<target>:nominal:baseline:1:ab12cd | <why the skill was invoked> | opencode/big-pickle#default / build | 4,120 | 380 | 210 | 12,400 | 0 | 17,110 | PASS |

## Token consumption per session

<!-- TOKENS_HISTOGRAM:START -->
| Date | Session ID | Phase | Scenario | Run | Children | Total tokens | Histogram |
| --- | --- | --- | --- | ---: | ---: | ---: | --- |
| 2026-10-23 | `ses_...` | baseline | nominal | 1 | 0 | 17,110 | ████████████████████████████████████████ |
Total sessions: 1 · max: 17,110 · scale: 1 block = 428 tokens
<!-- TOKENS_HISTOGRAM:END -->
```

## Rules

- The `## Runs` table is append-only. Rows are never edited or deleted, including
  rejected optimization runs.
- The histogram block between the markers is derived. It is regenerated after
  every run and printed after every optimization.
- Bars use a linear scale over the largest total, capped at 40 blocks. Exact
  totals are always printed, so the bar is only a visual aid.
- A session appears once, keyed by its root session ID. Child sessions are
  folded into the root total and counted in `Children`.
- A session total is rejected by the scripts when it cannot be read from
  OpenCode's recorded usage; no character-based estimate is ever substituted.
