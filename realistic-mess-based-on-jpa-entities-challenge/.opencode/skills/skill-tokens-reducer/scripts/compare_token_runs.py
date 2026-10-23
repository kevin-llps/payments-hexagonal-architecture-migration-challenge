#!/usr/bin/env python3
"""Compare baseline and candidate run directories.

A candidate is ACCEPTED only when every run passed, every declared artifact is
identical across phases, and every scenario's median total token count dropped
by at least the threshold. A significant increase or a failed result is a
REJECT; everything else is INCONCLUSIVE and must not be called a gain.

Usage:
    compare_token_runs.py --baseline <dir> --candidate <dir> [--threshold 5]

Exit codes: 0 = ACCEPT, 1 = REJECT or INCONCLUSIVE, 2 = usage/spec error.
"""

from __future__ import annotations

import argparse
import json
import statistics
import sys
from pathlib import Path
from typing import Any


class CompareError(RuntimeError):
    """Raised when run data is missing or inconsistent."""


def load_runs(directory: Path) -> list[dict[str, Any]]:
    root = directory.expanduser()
    if not root.is_dir():
        raise CompareError(f"run directory not found: {root}")
    runs: list[dict[str, Any]] = []
    for path in sorted(root.rglob("run-*.json")):
        try:
            run = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise CompareError(f"cannot read {path}: {error}") from error
        for field in ("scenario", "run", "result", "tokens"):
            if field not in run:
                raise CompareError(f"{path} is missing field: {field}")
        run["_path"] = path.as_posix()
        runs.append(run)
    if not runs:
        raise CompareError(f"no run-*.json under {root}")
    return runs


def group_by_scenario(runs: list[dict[str, Any]]) -> dict[str, list[dict[str, Any]]]:
    groups: dict[str, list[dict[str, Any]]] = {}
    for run in runs:
        groups.setdefault(str(run["scenario"]), []).append(run)
    for items in groups.values():
        items.sort(key=lambda item: int(item["run"]))
    return groups


def total_of(run: dict[str, Any]) -> int:
    tokens = run["tokens"]
    if "total" in tokens:
        return int(tokens["total"])
    cache = tokens.get("cache") or {}
    return (
        int(tokens.get("input") or 0)
        + int(tokens.get("output") or 0)
        + int(tokens.get("reasoning") or 0)
        + int(cache.get("read") or 0)
        + int(cache.get("write") or 0)
    )


def compare_artifacts(
    baseline: list[dict[str, Any]], candidate: list[dict[str, Any]]
) -> tuple[bool | None, str]:
    declared = [run.get("artifact_hashes") for run in baseline + candidate]
    if not any(declared):
        return None, "no artifacts declared"
    if any(value is None for value in declared):
        return False, "artifact hashes missing from at least one run"
    baseline_values = {
        json.dumps(value.get("artifact_hashes"), sort_keys=True) for value in baseline
    }
    candidate_values = {
        json.dumps(value.get("artifact_hashes"), sort_keys=True) for value in candidate
    }
    if len(baseline_values) != 1 or len(candidate_values) != 1:
        return False, "artifact hashes differ between runs of the same phase"
    if baseline_values != candidate_values:
        return False, "artifact hashes differ between baseline and candidate"
    return True, "artifacts identical"


def compare(
    baseline_runs: list[dict[str, Any]],
    candidate_runs: list[dict[str, Any]],
    threshold: float,
) -> dict[str, Any]:
    baseline = group_by_scenario(baseline_runs)
    candidate = group_by_scenario(candidate_runs)
    if set(baseline) != set(candidate):
        raise CompareError(
            "scenario sets differ: "
            f"baseline={sorted(baseline)} candidate={sorted(candidate)}"
        )
    tests_ok = True
    all_runs = baseline_runs + candidate_runs
    if any(run.get("target_tests_hash") is None for run in all_runs):
        tests_ok = False
    tests_hashes = [
        run.get("target_tests_hash")
        for run in all_runs
        if run.get("target_tests_hash") is not None
    ]
    if tests_hashes:
        serialized = {json.dumps(value, sort_keys=True) for value in tests_hashes}
        tests_ok = len(serialized) == 1
    models = {run.get("model") for run in all_runs}
    agents = {run.get("agent") for run in all_runs}
    targets = {run.get("target") for run in all_runs if run.get("target") is not None}
    scenario_hashes: dict[str, set[Any]] = {}
    for run in all_runs:
        scenario_hashes.setdefault(str(run["scenario"]), set()).add(
            run.get("scenario_hash")
        )
    settings_ok = (
        len(models) == 1
        and len(agents) == 1
        and len(targets) <= 1
        and all(len(values) == 1 and None not in values for values in scenario_hashes.values())
    )
    scenarios: list[dict[str, Any]] = []
    results_ok = True
    artifacts_ok = True
    regressions = False
    gains = True
    for name in sorted(baseline):
        before = baseline[name]
        after = candidate[name]
        if len(before) != len(after):
            raise CompareError(
                f"scenario {name}: {len(before)} baseline runs vs {len(after)} candidate runs"
            )
        before_median = statistics.median(total_of(run) for run in before)
        after_median = statistics.median(total_of(run) for run in after)
        delta = before_median - after_median
        delta_pct = (delta / before_median * 100) if before_median else 0.0
        passed = all(run.get("result") == "PASS" for run in before + after)
        results_ok = results_ok and passed
        artifact_state, artifact_note = compare_artifacts(before, after)
        if artifact_state is False:
            artifacts_ok = False
        if delta_pct <= -threshold:
            regressions = True
        if delta_pct < threshold:
            gains = False
        scenarios.append(
            {
                "scenario": name,
                "baseline_runs": len(before),
                "candidate_runs": len(after),
                "baseline_median": before_median,
                "candidate_median": after_median,
                "delta_tokens": delta,
                "delta_percent": round(delta_pct, 2),
                "results": "PASS" if passed else "FAIL",
                "artifacts": artifact_note,
            }
        )
    if not results_ok or not artifacts_ok or not tests_ok or not settings_ok or regressions:
        verdict = "REJECT"
    elif gains:
        verdict = "ACCEPT"
    else:
        verdict = "INCONCLUSIVE"
    return {
        "verdict": verdict,
        "threshold_percent": threshold,
        "results_ok": results_ok,
        "artifacts_ok": artifacts_ok,
        "tests_ok": tests_ok,
        "settings_ok": settings_ok,
        "scenarios": scenarios,
    }


def format_human(result: dict[str, Any]) -> str:
    lines = [
        f"Verdict: {result['verdict']} (threshold {result['threshold_percent']}%)",
        "Settings: model, agent, and scenario files "
        + ("unchanged" if result["settings_ok"] else "CHANGED"),
        "Target tests: " + ("unchanged" if result["tests_ok"] else "CHANGED"),
        "| Scenario | Runs | Baseline median | Candidate median | Delta | Result | Artifacts |",
        "| --- | ---: | ---: | ---: | ---: | --- | --- |",
    ]
    for item in result["scenarios"]:
        lines.append(
            "| {scenario} | {baseline_runs}/{candidate_runs} | {baseline_median:,.0f} | "
            "{candidate_median:,.0f} | {delta_percent:+.2f}% | {results} | {artifacts} |".format(
                **item
            )
        )
    return "\n".join(lines)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", required=True)
    parser.add_argument("--candidate", required=True)
    parser.add_argument("--threshold", type=float, default=5.0)
    parser.add_argument("--json", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        result = compare(
            load_runs(Path(args.baseline)),
            load_runs(Path(args.candidate)),
            args.threshold,
        )
    except CompareError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    if args.json:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print(format_human(result))
    return 0 if result["verdict"] == "ACCEPT" else 1


if __name__ == "__main__":
    raise SystemExit(main())
