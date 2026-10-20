#!/usr/bin/env python3
"""Enforce that a skill-forge run only creates one new skill tree.

Usage:
    python3 enforce-new-skill-scope.py snapshot [--root .opencode/skills]
    python3 enforce-new-skill-scope.py check --skill <id> [--root .opencode/skills]

`snapshot` records a content hash of every file under the root at launch.
`check` fails when a file that existed at snapshot time was modified or
deleted, or when a new file appeared outside <root>/<id>/.

Exit codes: 0 = clean, 1 = violation, 2 = usage or missing snapshot.
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import sys
import tempfile
from datetime import datetime
from pathlib import Path

SNAPSHOT_PATH = Path(tempfile.gettempdir()) / "opencode" / "skill-forge-baseline.json"
ID_PATTERN = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")

# scripts/ -> skill-testing/ -> skills/
SKILLS_ROOT = Path(__file__).resolve().parents[2]


def resolve_root(raw: str | None) -> Path:
    """Default to this script's skills root; otherwise resolve cwd then that root."""
    if raw is None:
        return SKILLS_ROOT
    candidate = Path(raw).expanduser()
    if candidate.is_absolute():
        return candidate.resolve()
    for base in (Path.cwd(), SKILLS_ROOT):
        resolved = (base / candidate).resolve()
        if resolved.exists():
            return resolved
    return (Path.cwd() / candidate).resolve()


def usage() -> None:
    print(
        "usage: python3 enforce-new-skill-scope.py snapshot [--root <dir>] | "
        "check --skill <id> [--root <dir>]",
        file=sys.stderr,
    )
    raise SystemExit(2)


def parse_args(argv: list[str]) -> tuple[str, Path, str | None]:
    if not argv:
        usage()
    command, rest = argv[0], argv[1:]
    if command not in ("snapshot", "check"):
        usage()
    root = None
    skill: str | None = None
    index = 0
    while index < len(rest):
        if rest[index] == "--root" and index + 1 < len(rest):
            root = rest[index + 1]
            index += 2
        elif rest[index] == "--skill" and index + 1 < len(rest):
            skill = rest[index + 1]
            index += 2
        else:
            usage()
    if command == "check":
        if not skill:
            usage()
        if not ID_PATTERN.match(skill):
            print(f"invalid skill id: {skill}", file=sys.stderr)
            raise SystemExit(2)
    return command, resolve_root(root), skill


def walk(directory: Path) -> list[Path]:
    if not directory.exists():
        return []
    return sorted(path for path in directory.rglob("*") if path.is_file())


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative_files(root: Path) -> dict[str, str]:
    return {
        path.relative_to(root).as_posix(): digest(path)
        for path in walk(root)
    }


def snapshot(root: Path) -> None:
    if not root.exists():
        print(f"guard error: root does not exist: {root}", file=sys.stderr)
        raise SystemExit(2)
    files = relative_files(root)
    SNAPSHOT_PATH.parent.mkdir(parents=True, exist_ok=True)
    SNAPSHOT_PATH.write_text(
        json.dumps(
            {
                "root": str(root),
                "takenAt": datetime.now().isoformat(),
                "files": files,
            },
            indent=2,
        ),
        encoding="utf-8",
    )
    print(f"snapshot: {len(files)} file(s) under {root}")
    print(f"saved: {SNAPSHOT_PATH}")


def check(root: Path, skill: str) -> None:
    if not SNAPSHOT_PATH.exists():
        print(f'guard error: no snapshot at {SNAPSHOT_PATH}; run "snapshot" first', file=sys.stderr)
        raise SystemExit(2)
    baseline = json.loads(SNAPSHOT_PATH.read_text(encoding="utf-8"))
    if baseline["root"] != str(root):
        print(f'guard error: snapshot root {baseline["root"]} does not match {root}', file=sys.stderr)
        raise SystemExit(2)

    current = relative_files(root)
    violations: list[str] = []

    for relative_path, expected in baseline["files"].items():
        if relative_path not in current:
            violations.append(f"deleted: {relative_path}")
        elif current[relative_path] != expected:
            violations.append(f"modified: {relative_path}")
    for relative_path in current:
        if relative_path not in baseline["files"] and not relative_path.startswith(f"{skill}/"):
            violations.append(f"created outside {skill}/: {relative_path}")

    print(f"guard: {root.name} / {skill}")
    if not violations:
        print("Result: CLEAN (existing skills untouched, new files confined)")
        raise SystemExit(0)
    for violation in violations:
        print(f"  - {violation}")
    print("Result: VIOLATION")
    raise SystemExit(1)


def main() -> None:
    command, root, skill = parse_args(sys.argv[1:])
    if command == "snapshot":
        snapshot(root)
    else:
        assert skill is not None
        check(root, skill)


if __name__ == "__main__":
    main()
