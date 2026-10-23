---
name: Skill Tokens Reducer
description: Measure the real tokens an OpenCode skill consumes across repeated isolated scenario runs, record them in TOKENS_HISTORY.md, and validate token-saving optimizations without changing skill results. Use when a skill's token consumption must be measured, reduced, or tracked over time.
---

# Skill Tokens Reducer

Measure first, optimize second, prove the result. Every token number comes from
OpenCode's recorded session usage; nothing is estimated.

## Purpose

Measure exactly one target skill over repeated, isolated scenario runs, maintain
its append-only token history and per-session histogram, and accept an
optimization only when the same scenarios consume fewer tokens and still produce
the same results.

`Total = input + cache.read + cache.write + output + reasoning`; OpenCode's
`input` is non-cached, so no token is counted twice.

## Scope

**In scope**

- Measuring one target skill per cycle.
- Inventing and persisting three scenarios, then running each at least three
  times per phase.
- Reading exact usage from OpenCode sessions, including child sessions.
- Maintaining `.opencode/skills/<target>/TOKENS_HISTORY.md`.
- Proposing and applying one optimization at a time, with snapshot and rollback.
- Re-running identical scenarios and comparing medians.

**Out of scope**

- Editing the target's `tests/` directory or weakening any assertion.
- Estimating tokens from characters, words, or a tokenizer guess.
- Committing, pushing, or publishing changes.
- Creating, renaming, or deleting the target skill.
- Judging agent-evaluation output automatically.

## Required inputs

1. **Target skill** - a kebab-case ID under `.opencode/skills/`.
2. **Model and agent** - pinned for every run; default to the current session
   model and the `build` agent.
3. **Budget** - defaults to 3 scenarios x 3 runs per phase.
4. **Threshold** - defaults to a 5% median total-token decrease.

Ask for a missing target in one batched question. Never invent a target.

## Workflow

1. Resolve `.opencode/skills/<target>/SKILL.md`; stop if it is missing.
2. Create `TOKENS_HISTORY.md` from `references/history-format.md` when absent.
   Never overwrite an existing history.
3. If `scenarios/<target>/` is empty, invent three scenarios (nominal, edge case,
   ambiguous input), save each as JSON using `references/scenario-format.md`, and
   make every prompt load the target skill explicitly. Reuse existing scenario
   files byte-identically on later cycles.
4. Run the baseline with
   `python3 .opencode/skills/skill-tokens-reducer/scripts/run_skill_scenarios.py --target <target> --phase baseline --runs 3 --model <provider/model#variant> --agent <agent>`.
   Use the current session's model and the `build` agent unless a scenario
   requires another. Every run uses a fresh isolated copy of the project, a
   unique session title, and the pinned model and agent.
5. Analyze the compact output of
   `scripts/summarize_session.py` for every run. Look for repeated identical
   tool calls, repeated file reads, hand-written deterministic steps, and
   duplicated instructions. Prefer replacing deterministic analysis with a
   script, then adding a guard instruction, then cutting duplicated wording. Do
   not read full transcripts.
6. Choose one optimization. Use
   `<cycle-dir> = .opencode/skills/skill-tokens-reducer/runs/<target>/opt-<n>` and
   snapshot the target with
   `scripts/fingerprint_workspace.py snapshot --skill-dir .opencode/skills/<target> --out <cycle-dir>/snapshot`.
   Apply the change to the target skill only; `tests/` must stay byte-identical.
7. Run the candidate phase with the same command and `--phase opt-<n>`, using the
   same scenario files, model, agent and run count.
8. Compare with
   `scripts/compare_token_runs.py --baseline <baseline-dir> --candidate <candidate-dir> --threshold 5 --json`
   and save the output as `<cycle-dir>/verdict.json`.
   Accept only when every scenario's median total drops by at least the threshold
   and every result check passes.
9. If accepted, keep the change. If rejected or inconclusive, restore the
   snapshot with
   `fingerprint_workspace.py restore --snapshot <cycle-dir>/snapshot --skill-dir .opencode/skills/<target>`,
   keep `verdict.json`, and record the rejection. Never edit history rows.
10. Print the histogram with
    `scripts/render_tokens_histogram.py --history .opencode/skills/<target>/TOKENS_HISTORY.md --print`
    after every optimization, accepted or rejected.
11. Record each run in history with the date, session title, invocation reason,
    session ID, token components and total. Report the before/after medians per
    scenario, the verdict, the histogram, and the history path.

## Rules

| ID  | Rule                                                                                                | Check            |
| --- | --------------------------------------------------------------------------------------------------- | ---------------- |
| R1  | TOKENS_HISTORY.md is created when absent and never overwritten                                      | static           |
| R2  | At least three scenarios are invented, persisted, and reused byte-identically                       | static           |
| R3  | Every measured run uses a fresh isolated copy and a unique session title                            | static           |
| R4  | The model and agent are pinned and identical before and after                                       | static           |
| R5  | Each history row records date, title, invocation reason, session ID, and tokens                     | static           |
| R6  | Token totals come from recorded session usage and are never estimated                               | static           |
| R7  | Child session tokens are included in every run total                                                | static           |
| R8  | The baseline runs each scenario at least three times                                                | static           |
| R9  | The target tests directory stays byte-identical                                                     | static           |
| R10 | The candidate phase reuses identical scenarios, model, agent, and run count                         | static           |
| R11 | Acceptance requires a median decrease of at least the threshold and all result checks passing       | static           |
| R12 | A rejected or inconclusive optimization is restored and recorded                                    | static           |
| R13 | History rows are append-only and never edited                                                       | static           |
| R14 | The histogram lists every session with its date and is printed after each optimization              | static           |
| R15 | The report shows before/after medians, verdict, histogram, and history path                         | static           |
| R16 | A missing target is reported and asked for before any measurement starts                            | agent-evaluation |
| R17 | No token saving is claimed or written without a measured comparison                                 | agent-evaluation |
| R18 | Every supporting script exists and has a matching test module                                       | static           |
| R19 | The skill is discoverable and its description states when to use it                                 | static           |

## Commands

```sh
# One measured phase: isolated runs, measurement, history append, histogram refresh
python3 .opencode/skills/skill-tokens-reducer/scripts/run_skill_scenarios.py \
  --target <target> --phase baseline --runs 3 \
  --model <provider/model#variant> --agent <agent>

# Exact usage of one run session, child sessions included
python3 .opencode/skills/skill-tokens-reducer/scripts/measure_session_tokens.py --session <session-id>

# Compact redundancy report instead of a full transcript
python3 .opencode/skills/skill-tokens-reducer/scripts/summarize_session.py --session <session-id>

# Before/after verdict from the two phase directories
python3 .opencode/skills/skill-tokens-reducer/scripts/compare_token_runs.py \
  --baseline <baseline-dir> --candidate <candidate-dir> --threshold 5

# Regenerate and print the per-session histogram
python3 .opencode/skills/skill-tokens-reducer/scripts/render_tokens_histogram.py \
  --history .opencode/skills/<target>/TOKENS_HISTORY.md --print
```

## Acceptance criteria

- [ ] A missing target is reported and no measurement starts.
- [ ] `TOKENS_HISTORY.md` exists before the first optimization and is never
      overwritten.
- [ ] Three persisted scenarios are reused byte-identically after an
      optimization.
- [ ] Every run is a fresh isolated session with the pinned model and agent.
- [ ] Every session total includes child-session tokens and matches OpenCode's
      recorded usage.
- [ ] The target `tests/` directory is byte-identical after an optimization.
- [ ] The before/after verdict requires a median drop of at least the threshold
      and every result check passing.
- [ ] A rejected optimization is reverted and remains visible in history.
- [ ] The histogram lists every session with its date and is printed after every
      optimization.
- [ ] No saving is claimed without baseline and candidate runs.
