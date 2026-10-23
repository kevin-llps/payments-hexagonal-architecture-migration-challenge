# Skill Tokens Reducer

Measures the real tokens one OpenCode skill consumes across repeated isolated
scenarios, records each run in that skill's `TOKENS_HISTORY.md`, and keeps a
token-saving change only when the same scenarios get cheaper without changing
results. Every total comes from OpenCode's recorded session usage, child
sessions included.

## When to use

Use when a skill's token consumption must be measured, reduced, or tracked over
time.

## How to run

From the project root, ask for one target skill (for example
"measure and reduce skill-updater"). The skill runs the baseline, proposes one
optimization, re-runs the same scenarios, and prints the per-session histogram
after the cycle.

```sh
python3 .opencode/skills/skill-tokens-reducer/scripts/run_skill_scenarios.py \
  --target <target> --phase baseline --runs 3 \
  --model <provider/model#variant> --agent <agent>
```

## Dependency graph

No agent or skill calls this skill yet; it is invoked directly by the user from
a build session.

## Workflow graph

```mermaid
graph TD
    S1["1: Resolve the target SKILL.md"] --> S2["2: Create TOKENS_HISTORY.md when absent"]
    S2 -.-> F1["builds: .opencode/skills/{target}/TOKENS_HISTORY.md"]
    S2 --> S3["3: Invent or reuse three scenario JSON files"]
    S3 -.-> F2["builds: scenarios/{target}/*.json"]
    S3 --> S4["4: Run the baseline - isolated, pinned, 3 runs"]
    S4 --> S5["5: Summarize sessions for redundancy signals"]
    S5 --> S6["6: Snapshot target and apply one optimization"]
    S6 -.-> F3["builds: runs/{target}/{cycle}/snapshot/"]
    S6 --> S7["7: Run the candidate phase with identical settings"]
    S7 --> S8["8: Compare medians and result checks"]
    S8 --> S9{"9: Accepted?"}
    S9 -->|no| S10["10: Restore the snapshot and record the rejection"]
    S9 -->|yes| S11["11: Print the histogram and report"]
    S10 --> S11
```
