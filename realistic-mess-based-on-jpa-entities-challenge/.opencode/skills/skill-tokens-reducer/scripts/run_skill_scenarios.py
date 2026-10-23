#!/usr/bin/env python3
"""Run one measured phase for a target skill.

For every persisted scenario and every repetition this script:
1. copies the project into a fresh sandbox,
2. invokes the skill through ``opencode run --format json`` with a pinned model
   and agent and a unique session title,
3. measures the exact session usage (children included),
4. evaluates scenario assertions and the target's own rule tests,
5. writes a run JSON, appends the history row, and refreshes the histogram.

Usage:
    run_skill_scenarios.py --target skill-updater --phase baseline --runs 3 \
        --model opencode/big-pickle#default --agent build

Exit codes: 0 = every run passed, 1 = at least one run failed, 2 = usage error.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
import time
from datetime import datetime
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).resolve().parent))
import render_tokens_histogram as histogram  # noqa: E402

SCRIPT_DIR = Path(__file__).resolve().parent
MEASURE_SCRIPT = SCRIPT_DIR / "measure_session_tokens.py"
APPEND_SCRIPT = SCRIPT_DIR / "append_tokens_history.py"
RENDER_SCRIPT = SCRIPT_DIR / "render_tokens_histogram.py"
DEFAULT_EXCLUDES = {
    ".git",
    ".idea",
    "target",
    "node_modules",
    "__pycache__",
    ".DS_Store",
    "opencode.db",
    "opencode.db-shm",
    "opencode.db-wal",
}


class RunnerError(RuntimeError):
    """Raised when a phase cannot be started or completed."""


def sanitize_phase(phase: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._-]+", "-", phase).strip("-")
    if not cleaned:
        raise RunnerError(f"invalid phase: {phase!r}")
    return cleaned


def unique_title(target: str, scenario: str, phase: str, run: int) -> str:
    return f"tokens:{target}:{scenario}:{phase}:{run}:{secrets.token_hex(3)}"


def build_command(
    opencode_bin: str, agent: str, model: str, title: str, prompt: str
) -> list[str]:
    return [
        opencode_bin,
        "run",
        "--standalone",
        "--format",
        "json",
        "--agent",
        agent,
        "--model",
        model,
        "--title",
        title,
        "--auto",
        prompt,
    ]


def sandbox_env(sandbox: Path) -> dict[str, str]:
    """Keep the child's working directory consistent with its real cwd."""
    environment = dict(os.environ)
    environment["PWD"] = str(sandbox)
    environment["OLDPWD"] = str(sandbox)
    return environment


def parse_events(stdout: str) -> dict[str, Any]:
    session_id = ""
    texts: list[str] = []
    errors: list[str] = []
    for line in stdout.splitlines():
        line = line.strip()
        if not line:
            continue
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        if not isinstance(event, dict):
            continue
        session_id = session_id or str(event.get("sessionID") or "")
        part = event.get("part")
        if event.get("type") == "text" and isinstance(part, dict):
            texts.append(str(part.get("text") or ""))
        if event.get("type") == "error":
            errors.append(str(event.get("error") or event))
    return {"session_id": session_id, "text": "\n".join(texts), "errors": errors}


def find_project_root(start: Path) -> Path:
    completed = subprocess.run(
        ["git", "-C", str(start), "rev-parse", "--show-toplevel"],
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode == 0 and completed.stdout.strip():
        candidate = Path(completed.stdout.strip())
        if (candidate / ".opencode" / "skills").is_dir():
            return candidate
    return start.resolve()


def copy_project(source: Path, destination: Path) -> None:
    def ignore(directory: str, names: list[str]) -> set[str]:
        relative = Path(directory).resolve().relative_to(source.resolve())
        ignored = {name for name in names if name in DEFAULT_EXCLUDES}
        if relative.as_posix() == ".opencode/skills/skill-tokens-reducer" and "runs" in names:
            ignored.add("runs")
        return ignored

    shutil.copytree(source, destination, ignore=ignore, dirs_exist_ok=True)


def load_scenarios(directory: Path) -> list[dict[str, Any]]:
    if not directory.is_dir():
        raise RunnerError(f"scenarios directory not found: {directory}")
    scenarios: list[dict[str, Any]] = []
    for path in sorted(directory.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise RunnerError(f"cannot read scenario {path}: {error}") from error
        if not isinstance(data, dict) or not isinstance(data.get("prompt"), str) or not data["prompt"].strip():
            raise RunnerError(f"scenario needs a non-empty string prompt: {path}")
        data.setdefault("id", path.stem)
        data.setdefault("reason", data.get("prompt", ""))
        data.setdefault("assertions", {})
        if not isinstance(data["id"], str) or not isinstance(data["assertions"], dict):
            raise RunnerError(f"scenario id/assertions have the wrong type: {path}")
        if not re.fullmatch(r"[a-z0-9][a-z0-9._-]*", data["id"]):
            raise RunnerError(f"invalid scenario id: {data['id']!r}")
        for field in ("model", "agent"):
            if field in data and not isinstance(data[field], str):
                raise RunnerError(f"scenario {field} must be a string: {path}")
        data["_path"] = path.resolve().as_posix()
        scenarios.append(data)
    if not scenarios:
        raise RunnerError(f"no scenario JSON files in {directory}")
    return scenarios


def hash_file(path: Path) -> str:
    hasher = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def tree_hashes(directory: Path) -> dict[str, str]:
    if not directory.is_dir():
        return {}
    return {
        path.relative_to(directory).as_posix(): hash_file(path)
        for path in sorted(directory.rglob("*"))
        if path.is_file() and "__pycache__" not in path.parts
    }


def evaluate_assertions(
    sandbox: Path,
    target: str,
    assertions: dict[str, Any],
    final_text: str,
    timeout: int,
    skip_rule_tests: bool = False,
) -> tuple[bool, list[dict[str, str]], dict[str, str]]:
    details: list[dict[str, str]] = []

    def record(name: str, passed: bool, detail: str) -> None:
        details.append(
            {"check": name, "status": "PASS" if passed else "FAIL", "detail": detail}
        )

    for relative in assertions.get("files", []):
        path = sandbox / relative
        record(f"file:{relative}", path.is_file(), "exists" if path.is_file() else "missing")

    for item in assertions.get("contains", []):
        path = sandbox / str(item.get("path", ""))
        expected = str(item.get("text", ""))
        try:
            actual = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            actual = ""
        passed = expected in actual
        record(
            f"contains:{item.get('path')}",
            passed,
            f"found {expected!r}" if passed else f"missing {expected!r}",
        )

    for item in assertions.get("not_contains", []):
        path = sandbox / str(item.get("path", ""))
        forbidden = str(item.get("text", ""))
        try:
            actual = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            actual = ""
        passed = forbidden not in actual
        record(
            f"not_contains:{item.get('path')}",
            passed,
            f"absent {forbidden!r}" if passed else f"forbidden text present {forbidden!r}",
        )

    for expected in assertions.get("final_text_contains", []):
        passed = str(expected) in final_text
        record(
            f"final_text:{expected}",
            passed,
            "found" if passed else f"missing {expected!r}",
        )

    for index, command in enumerate(assertions.get("commands", [])):
        use_shell = isinstance(command, str)
        try:
            completed = subprocess.run(
                command,
                cwd=sandbox,
                shell=use_shell,
                check=False,
                capture_output=True,
                text=True,
                timeout=timeout,
            )
            passed = completed.returncode == 0
            tail = (completed.stderr or completed.stdout or "").strip()[-500:]
            record(f"command:{index}", passed, tail or f"exit {completed.returncode}")
        except (OSError, subprocess.TimeoutExpired) as error:
            record(f"command:{index}", False, str(error))

    skill_dir = sandbox / ".opencode" / "skills" / target
    has_rule_tests = (skill_dir / "tests" / "rules.json").is_file()
    run_tests = assertions.get("rule_tests", has_rule_tests) and not skip_rule_tests
    if run_tests:
        runner = (
            sandbox
            / ".opencode"
            / "skills"
            / "skill-testing"
            / "scripts"
            / "run-skill-rule-tests.py"
        )
        if not runner.is_file():
            record("rule_tests", False, f"runner missing: {runner}")
        else:
            completed = subprocess.run(
                [sys.executable, str(runner), str(skill_dir)],
                cwd=sandbox,
                check=False,
                capture_output=True,
                text=True,
                timeout=timeout,
            )
            tail = (completed.stdout or "").strip()[-1200:]
            record(
                "rule_tests",
                completed.returncode == 0,
                (completed.stderr or tail or "").strip()[-800:],
            )

    artifacts: dict[str, str] = {}
    for relative in assertions.get("artifacts", []):
        path = sandbox / relative
        if path.is_file():
            artifacts[relative] = hash_file(path)
        else:
            record(f"artifact:{relative}", False, "missing")
    return all(item["status"] == "PASS" for item in details), details, artifacts


def measure(session_id: str, title: str, timeout: int) -> dict[str, Any]:
    command = [sys.executable, str(MEASURE_SCRIPT), "--json"]
    if session_id:
        command += ["--session", session_id]
    else:
        command += ["--title", title]
    completed = subprocess.run(
        command, check=False, capture_output=True, text=True, timeout=timeout
    )
    if completed.returncode != 0:
        raise RunnerError(
            f"measurement failed: {(completed.stderr or completed.stdout).strip()}"
        )
    return json.loads(completed.stdout)


def append_history(history: Path, run_path: Path, target: str) -> None:
    completed = subprocess.run(
        [
            sys.executable,
            str(APPEND_SCRIPT),
            "--history",
            str(history),
            "--run-json",
            str(run_path),
            "--target",
            target,
        ],
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        raise RunnerError(
            f"history append failed: {(completed.stderr or completed.stdout).strip()}"
        )


def ensure_history(history: Path, target: str) -> None:
    if not history.is_file():
        history.parent.mkdir(parents=True, exist_ok=True)
        history.write_text(histogram.new_history(target), encoding="utf-8")


def run_phase(args: argparse.Namespace) -> int:
    project_root = (
        Path(args.project_root).expanduser().resolve()
        if args.project_root
        else find_project_root(Path.cwd())
    )
    skills_root = (
        Path(args.skills_root).expanduser().resolve()
        if args.skills_root
        else project_root / ".opencode" / "skills"
    )
    skill_tokens_dir = Path(__file__).resolve().parents[1]
    target = args.target
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", target):
        raise RunnerError(f"invalid target skill id: {target!r}")
    target_dir = skills_root / target
    if not (target_dir / "SKILL.md").is_file():
        raise RunnerError(f"target skill not found: {target_dir / 'SKILL.md'}")
    phase = sanitize_phase(args.phase)
    scenarios_dir = (
        Path(args.scenarios_dir).expanduser().resolve()
        if args.scenarios_dir
        else skill_tokens_dir / "scenarios" / target
    )
    runs_dir = (
        Path(args.runs_dir).expanduser().resolve()
        if args.runs_dir
        else skill_tokens_dir / "runs" / target / phase
    )
    history = (
        Path(args.history).expanduser().resolve()
        if args.history
        else target_dir / "TOKENS_HISTORY.md"
    )
    scenarios = load_scenarios(scenarios_dir)

    if args.dry_run:
        for scenario in scenarios:
            model = args.model or scenario.get("model")
            agent = args.agent or scenario.get("agent") or "build"
            if not model:
                raise RunnerError(
                    "pass --model or set model in every scenario "
                    "(for example opencode/big-pickle#default)"
                )
            for index in range(1, args.runs + 1):
                title = unique_title(target, scenario["id"], phase, index)
                print(
                    f"[dry-run] {scenario['id']} run {index}: "
                    f"model={model} agent={agent} title={title} "
                    f"prompt={scenario['prompt'][:80]!r}"
                )
        print(f"[dry-run] scenarios: {len(scenarios)}, runs each: {args.runs}, phase: {phase}")
        return 0

    ensure_history(history, target)
    runs_dir.mkdir(parents=True, exist_ok=True)
    failures = 0
    for scenario in scenarios:
        model = args.model or scenario.get("model")
        agent = args.agent or scenario.get("agent") or "build"
        if not model:
            raise RunnerError(
                "pass --model or set model in every scenario "
                "(for example opencode/big-pickle#default)"
            )
        scenario_dir = runs_dir / scenario["id"]
        scenario_dir.mkdir(parents=True, exist_ok=True)
        for index in range(1, args.runs + 1):
            title = unique_title(target, scenario["id"], phase, index)
            command = build_command(
                args.opencode_bin, agent, model, title, scenario["prompt"]
            )
            sandbox_parent = Path(
                tempfile.mkdtemp(prefix=f"skill-tokens-{target}-{scenario['id']}-")
            )
            sandbox = sandbox_parent / "project"
            started = time.monotonic()
            stdout = ""
            stderr = ""
            exit_code = -1
            measured: dict[str, Any] = {}
            final_text = ""
            details: list[dict[str, str]] = []
            artifacts: dict[str, str] = {}
            event_errors: list[str] = []
            passed = False
            error_message = ""
            try:
                copy_project(project_root, sandbox)
                completed = subprocess.run(
                    command,
                    cwd=sandbox,
                    env=sandbox_env(sandbox),
                    check=False,
                    capture_output=True,
                    text=True,
                    timeout=args.timeout,
                )
                stdout, stderr = completed.stdout, completed.stderr
                exit_code = completed.returncode
                parsed = parse_events(stdout)
                final_text = parsed["text"]
                event_errors = parsed["errors"]
                measured = measure(
                    parsed["session_id"], title, args.timeout
                )
                assertions_ok, details, artifacts = evaluate_assertions(
                    sandbox,
                    target,
                    scenario.get("assertions") or {},
                    final_text,
                    args.timeout,
                    args.skip_rule_tests,
                )
                passed = exit_code == 0 and assertions_ok and not event_errors
                if not passed:
                    error_message = "; ".join(event_errors) or "session or assertions failed"
            except (OSError, RunnerError, subprocess.TimeoutExpired, json.JSONDecodeError) as error:
                error_message = str(error)
                details.append(
                    {"check": "run", "status": "FAIL", "detail": error_message}
                )
            finally:
                if not args.keep_sandbox:
                    shutil.rmtree(sandbox_parent, ignore_errors=True)
                else:
                    print(f"kept sandbox: {sandbox_parent}")

            duration = round(time.monotonic() - started, 2)
            tokens = (measured.get("tokens") or {}) if measured else {}
            run_record = {
                "target": target,
                "phase": phase,
                "scenario": scenario["id"],
                "scenario_hash": hash_file(Path(scenario["_path"])),
                "run": index,
                "title": title,
                "reason": scenario.get("reason", ""),
                "date": measured.get("date") or datetime.now().astimezone().strftime("%Y-%m-%d"),
                "session_id": measured.get("session_id", ""),
                "children": measured.get("children", 0),
                "model": measured.get("model_label") or model,
                "agent": measured.get("agent") or agent,
                "tokens": tokens,
                "cost": measured.get("cost", 0.0),
                "duration_seconds": duration,
                "exit_code": exit_code,
                "result": "PASS" if passed else "FAIL",
                "assertions": details,
                "artifact_hashes": artifacts,
                "target_tests_hash": tree_hashes(target_dir / "tests"),
                "final_text": final_text[-4000:],
                "error": error_message,
                "event_errors": event_errors,
                "command": command,
            }
            run_path = scenario_dir / f"run-{index}.json"
            run_path.write_text(
                json.dumps(run_record, indent=2, ensure_ascii=False), encoding="utf-8"
            )
            if run_record["session_id"]:
                append_history(history, run_path, target)
            print(
                f"{run_record['result']} {target} {scenario['id']} run {index} "
                f"total={int((tokens or {}).get('total') or 0):,} "
                f"session={run_record['session_id'] or '?'}"
            )
            if not passed:
                failures += 1

    print(f"phase {phase}: {len(scenarios) * args.runs} run(s), {failures} failure(s)")
    rows = histogram.rewrite_history(history)
    histogram.print_rows(rows)
    print(f"history: {history}")
    print(f"runs:    {runs_dir}")
    return 0 if failures == 0 else 1


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", required=True)
    parser.add_argument("--phase", required=True)
    parser.add_argument("--runs", type=int, default=3)
    parser.add_argument("--model")
    parser.add_argument("--agent")
    parser.add_argument("--project-root")
    parser.add_argument("--skills-root")
    parser.add_argument("--scenarios-dir")
    parser.add_argument("--runs-dir")
    parser.add_argument("--history")
    parser.add_argument("--opencode-bin", default="opencode")
    parser.add_argument("--timeout", type=int, default=900)
    parser.add_argument("--skip-rule-tests", action="store_true")
    parser.add_argument("--keep-sandbox", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    if args.runs < 1:
        print("error: --runs must be at least 1", file=sys.stderr)
        return 2
    if args.timeout < 1:
        print("error: --timeout must be at least 1 second", file=sys.stderr)
        return 2
    try:
        return run_phase(args)
    except RunnerError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
