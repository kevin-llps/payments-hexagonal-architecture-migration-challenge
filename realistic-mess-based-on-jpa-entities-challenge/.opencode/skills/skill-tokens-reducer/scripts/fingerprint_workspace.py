#!/usr/bin/env python3
"""Snapshot and compare skill trees deterministically.

Subcommands:
    hash-tree --dir <path> [--out <file>] [--exclude name ...]
    compare --before <manifest> --after <manifest> [--fail-on-diff]
    snapshot --skill-dir <path> --out <dir> [--force]
    restore --snapshot <dir> --skill-dir <path>

A snapshot copies the skill source next to a manifest. Restoring removes the
current source files, copies the snapshot back, and preserves
TOKENS_HISTORY.md so rejected optimization rows stay visible.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

DEFAULT_EXCLUDES = {
    ".git",
    ".idea",
    "target",
    "node_modules",
    "__pycache__",
    ".DS_Store",
    "opencode.db",
}
PRESERVE_ON_RESTORE = {"TOKENS_HISTORY.md"}


class FingerprintError(RuntimeError):
    """Raised when a tree cannot be read or compared."""


def digest(path: Path) -> str:
    hasher = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def hash_tree(directory: Path, excludes: set[str] | None = None) -> dict[str, Any]:
    root = directory.expanduser().resolve()
    if not root.is_dir():
        raise FingerprintError(f"directory not found: {root}")
    blocked = DEFAULT_EXCLUDES | (excludes or set())
    files: dict[str, str] = {}
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(root)
        if any(part in blocked for part in relative.parts):
            continue
        files[relative.as_posix()] = digest(path)
    return {"root": root.as_posix(), "count": len(files), "files": files}


def compare_manifests(before: dict[str, Any], after: dict[str, Any]) -> dict[str, Any]:
    old = before.get("files") or {}
    new = after.get("files") or {}
    added = sorted(set(new) - set(old))
    removed = sorted(set(old) - set(new))
    changed = sorted(path for path in set(old) & set(new) if old[path] != new[path])
    return {
        "equal": not (added or removed or changed),
        "added": added,
        "removed": removed,
        "changed": changed,
        "before_count": len(old),
        "after_count": len(new),
    }


def copy_source(source: Path, destination: Path, skip_history: bool = True) -> None:
    def ignore(_directory: str, names: list[str]) -> set[str]:
        return {name for name in names if skip_history and name in PRESERVE_ON_RESTORE}

    shutil.copytree(source, destination, ignore=ignore, dirs_exist_ok=True)


def snapshot(skill_dir: Path, out: Path, force: bool = False) -> dict[str, Any]:
    source = skill_dir.expanduser().resolve()
    if not (source / "SKILL.md").is_file():
        raise FingerprintError(f"not a skill directory: {source}")
    target = out.expanduser().resolve()
    if target.exists():
        if not force:
            raise FingerprintError(f"snapshot target already exists: {target}")
        shutil.rmtree(target)
    target.mkdir(parents=True)
    copy_source(source, target / "skill")
    manifest = hash_tree(source)
    (target / "manifest.json").write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    return {"snapshot": target.as_posix(), "manifest": manifest}


def restore(snapshot_dir: Path, skill_dir: Path) -> dict[str, Any]:
    source = snapshot_dir.expanduser().resolve()
    saved = source / "skill"
    if not saved.is_dir():
        raise FingerprintError(f"snapshot has no skill/ directory: {source}")
    target = skill_dir.expanduser().resolve()
    target.mkdir(parents=True, exist_ok=True)
    for item in target.iterdir():
        if item.name in PRESERVE_ON_RESTORE:
            continue
        if item.is_dir():
            shutil.rmtree(item)
        else:
            item.unlink()
    copy_source(saved, target)
    manifest = hash_tree(target)
    return {"restored": target.as_posix(), "from": source.as_posix(), "manifest": manifest}


def read_manifest(path: Path) -> dict[str, Any]:
    try:
        return json.loads(path.expanduser().read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise FingerprintError(f"cannot read manifest {path}: {error}") from error


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    hashing = subparsers.add_parser("hash-tree")
    hashing.add_argument("--dir", required=True)
    hashing.add_argument("--out")
    hashing.add_argument("--exclude", action="append", default=[])

    comparing = subparsers.add_parser("compare")
    comparing.add_argument("--before", required=True)
    comparing.add_argument("--after", required=True)
    comparing.add_argument("--fail-on-diff", action="store_true")

    taking = subparsers.add_parser("snapshot")
    taking.add_argument("--skill-dir", required=True)
    taking.add_argument("--out", required=True)
    taking.add_argument("--force", action="store_true")

    restoring = subparsers.add_parser("restore")
    restoring.add_argument("--snapshot", required=True)
    restoring.add_argument("--skill-dir", required=True)
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    try:
        if args.command == "hash-tree":
            result = hash_tree(Path(args.dir), set(args.exclude))
            if args.out:
                Path(args.out).expanduser().write_text(
                    json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8"
                )
                print(f"manifest: {args.out} ({result['count']} files)")
            else:
                print(json.dumps(result, indent=2, ensure_ascii=False))
            return 0
        if args.command == "compare":
            result = compare_manifests(
                read_manifest(Path(args.before)), read_manifest(Path(args.after))
            )
            print(json.dumps(result, indent=2, ensure_ascii=False))
            return 1 if args.fail_on_diff and not result["equal"] else 0
        if args.command == "snapshot":
            result = snapshot(Path(args.skill_dir), Path(args.out), args.force)
            print(
                f"snapshot: {result['snapshot']} "
                f"({result['manifest']['count']} files)"
            )
            return 0
        result = restore(Path(args.snapshot), Path(args.skill_dir))
        print(f"restored: {result['restored']} from {result['from']}")
        return 0
    except FingerprintError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
