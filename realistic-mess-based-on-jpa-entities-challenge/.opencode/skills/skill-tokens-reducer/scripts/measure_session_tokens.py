#!/usr/bin/env python3
"""Read the exact token usage OpenCode recorded for one session tree.

The session identified by ``--session`` or ``--title`` is the root. Every
descendant session (subagents) is included in the total. Usage comes from the
``session_v2`` table, never from a transcript estimate.

Usage:
    measure_session_tokens.py --session ses_... [--json]
    measure_session_tokens.py --title "exact session title" [--json]

Exit codes: 0 = measured, 1 = measurement failed, 2 = usage error.
"""

from __future__ import annotations

import argparse
import json
import os
import sqlite3
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import Any

TOKEN_COLUMNS = (
    ("input", "tokens_input"),
    ("output", "tokens_output"),
    ("reasoning", "tokens_reasoning"),
    ("cache_read", "tokens_cache_read"),
    ("cache_write", "tokens_cache_write"),
)


class MeasurementError(RuntimeError):
    """Raised when OpenCode usage cannot be read."""


def default_db_path() -> Path:
    explicit = os.environ.get("OPENCODE_DB")
    if explicit:
        return Path(explicit).expanduser()
    data_dir = os.environ.get("OPENCODE_DATA_DIR")
    if data_dir:
        return Path(data_dir).expanduser() / "opencode.db"
    xdg = os.environ.get("XDG_DATA_HOME")
    base = Path(xdg).expanduser() if xdg else Path.home() / ".local" / "share"
    return base / "opencode" / "opencode.db"


def connect_readonly(db_path: Path) -> sqlite3.Connection:
    if not db_path.is_file():
        raise MeasurementError(f"OpenCode database not found: {db_path}")
    connection = sqlite3.connect(f"file:{db_path}?mode=ro", uri=True)
    connection.row_factory = sqlite3.Row
    return connection


def choose_table(connection: sqlite3.Connection) -> str:
    names = {
        row[0]
        for row in connection.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        )
    }
    for table in ("session_v2", "session"):
        if table in names:
            return table
    raise MeasurementError("neither session_v2 nor session exists in the database")


def find_root(
    connection: sqlite3.Connection,
    table: str,
    session_id: str | None,
    title: str | None,
) -> sqlite3.Row:
    if session_id:
        row = connection.execute(
            f"SELECT * FROM {table} WHERE id = ?", (session_id,)
        ).fetchone()
        if row is None:
            raise MeasurementError(f"session not found: {session_id}")
        return row
    if title:
        rows = connection.execute(
            f"SELECT * FROM {table} WHERE title = ?", (title,)
        ).fetchall()
        if not rows:
            raise MeasurementError(f"no session titled: {title}")
        if len(rows) > 1:
            ids = ", ".join(row["id"] for row in rows)
            raise MeasurementError(f"title matches several sessions: {ids}")
        return rows[0]
    raise MeasurementError("pass --session or --title")


def tree_rows(
    connection: sqlite3.Connection, table: str, root_id: str
) -> list[sqlite3.Row]:
    query = f"""
        WITH RECURSIVE tree(id) AS (
            SELECT ?
            UNION ALL
            SELECT child.id
            FROM {table} AS child
            JOIN tree ON child.parent_id = tree.id
        )
        SELECT * FROM {table}
        WHERE id IN (SELECT id FROM tree)
        ORDER BY time_created, id
    """
    return connection.execute(query, (root_id,)).fetchall()


def iso_parts(milliseconds: int | None) -> tuple[str, str]:
    if not milliseconds:
        return "", ""
    moment = datetime.fromtimestamp(milliseconds / 1000).astimezone()
    return moment.strftime("%Y-%m-%d"), moment.isoformat(timespec="seconds")


def model_label(model: Any) -> str:
    if isinstance(model, dict):
        provider = model.get("providerID") or model.get("provider") or ""
        name = model.get("id") or model.get("modelID") or ""
        label = f"{provider}/{name}" if provider else str(name)
        variant = model.get("variant")
        if variant and variant != "default":
            label = f"{label}#{variant}"
        return label
    if model is None:
        return ""
    return str(model)


def parse_model(raw: Any) -> Any:
    if isinstance(raw, str) and raw.strip().startswith("{"):
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return raw
    return raw


def session_tokens(row: sqlite3.Row) -> dict[str, Any]:
    values = {name: int(row[column] or 0) for name, column in TOKEN_COLUMNS}
    total = sum(values.values())
    return {
        "input": values["input"],
        "output": values["output"],
        "reasoning": values["reasoning"],
        "cache": {"read": values["cache_read"], "write": values["cache_write"]},
        "total": total,
    }


def session_entry(row: sqlite3.Row) -> dict[str, Any]:
    date, created_at = iso_parts(row["time_created"])
    tokens = session_tokens(row)
    return {
        "id": row["id"],
        "parent_id": row["parent_id"],
        "title": row["title"],
        "date": date,
        "created_at": created_at,
        "agent": row["agent"],
        "model": parse_model(row["model"]),
        "directory": row["directory"],
        "cost": float(row["cost"] or 0.0),
        "tokens": tokens,
    }


def measure(
    connection: sqlite3.Connection,
    session_id: str | None = None,
    title: str | None = None,
) -> dict[str, Any]:
    table = choose_table(connection)
    root = find_root(connection, table, session_id, title)
    rows = tree_rows(connection, table, root["id"])
    sessions = [session_entry(row) for row in rows]
    totals = {name: 0 for name, _ in TOKEN_COLUMNS}
    for entry in sessions:
        tokens = entry["tokens"]
        totals["input"] += tokens["input"]
        totals["output"] += tokens["output"]
        totals["reasoning"] += tokens["reasoning"]
        totals["cache_read"] += tokens["cache"]["read"]
        totals["cache_write"] += tokens["cache"]["write"]
    total = sum(totals.values())
    root_entry = session_entry(root)
    return {
        "session_id": root_entry["id"],
        "title": root_entry["title"],
        "date": root_entry["date"],
        "created_at": root_entry["created_at"],
        "model": root_entry["model"],
        "model_label": model_label(root_entry["model"]),
        "agent": root_entry["agent"],
        "directory": root_entry["directory"],
        "children": len(sessions) - 1,
        "sessions": sessions,
        "tokens": {
            "input": totals["input"],
            "output": totals["output"],
            "reasoning": totals["reasoning"],
            "cache": {
                "read": totals["cache_read"],
                "write": totals["cache_write"],
            },
            "total": total,
        },
        "cost": sum(entry["cost"] for entry in sessions),
        "measured_from": "database",
    }


def run_json(command: list[str]) -> Any:
    completed = subprocess.run(
        command, check=False, capture_output=True, text=True
    )
    if completed.returncode != 0:
        message = completed.stderr.strip() or completed.stdout.strip()
        raise MeasurementError(
            f"command failed ({completed.returncode}): {' '.join(command)}: {message}"
        )
    try:
        return json.loads(completed.stdout)
    except json.JSONDecodeError as error:
        raise MeasurementError(f"invalid JSON from {' '.join(command)}: {error}")


def measure_via_export(
    opencode_bin: str, session_id: str | None, title: str | None
) -> dict[str, Any]:
    """Fallback for older databases: export one top-level session."""
    if not session_id:
        if not title:
            raise MeasurementError("pass --session or --title")
        sessions = run_json([opencode_bin, "session", "list", "--format", "json"])
        matches = [item for item in sessions if item.get("title") == title]
        if not matches:
            raise MeasurementError(f"no session titled: {title}")
        if len(matches) > 1:
            ids = ", ".join(item["id"] for item in matches)
            raise MeasurementError(f"title matches several sessions: {ids}")
        session_id = matches[0]["id"]
    payload = run_json([opencode_bin, "session", "export", session_id])
    info = payload.get("info") or {}
    tokens = info.get("tokens") or {}
    cache = tokens.get("cache") or {}
    date, created_at = iso_parts((info.get("time") or {}).get("created"))
    model = info.get("model")
    entry = {
        "id": info.get("id", session_id),
        "parent_id": info.get("parentID"),
        "title": info.get("title", ""),
        "date": date,
        "created_at": created_at,
        "agent": info.get("agent"),
        "model": model,
        "directory": (info.get("location") or {}).get("directory", ""),
        "cost": float(info.get("cost") or 0.0),
        "tokens": {
            "input": int(tokens.get("input") or 0),
            "output": int(tokens.get("output") or 0),
            "reasoning": int(tokens.get("reasoning") or 0),
            "cache": {
                "read": int(cache.get("read") or 0),
                "write": int(cache.get("write") or 0),
            },
        },
    }
    entry["tokens"]["total"] = sum(
        [
            entry["tokens"]["input"],
            entry["tokens"]["output"],
            entry["tokens"]["reasoning"],
            entry["tokens"]["cache"]["read"],
            entry["tokens"]["cache"]["write"],
        ]
    )
    return {
        "session_id": entry["id"],
        "title": entry["title"],
        "date": entry["date"],
        "created_at": entry["created_at"],
        "model": model,
        "model_label": model_label(model),
        "agent": entry["agent"],
        "directory": entry["directory"],
        "children": 0,
        "sessions": [entry],
        "tokens": entry["tokens"],
        "cost": entry["cost"],
        "measured_from": "export",
    }


def format_human(result: dict[str, Any]) -> str:
    tokens = result["tokens"]
    return "\n".join(
        [
            f"session: {result['session_id']}",
            f"title:   {result['title']}",
            f"date:    {result['date']}",
            f"model:   {result['model_label']} / {result['agent']}",
            f"children: {result['children']}",
            (
                "tokens:  "
                f"input={tokens['input']:,} "
                f"output={tokens['output']:,} "
                f"reasoning={tokens['reasoning']:,} "
                f"cache.read={tokens['cache']['read']:,} "
                f"cache.write={tokens['cache']['write']:,} "
                f"total={tokens['total']:,}"
            ),
        ]
    )


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--session", help="root OpenCode session ID")
    source.add_argument("--title", help="exact root session title")
    parser.add_argument("--db", help="path to opencode.db")
    parser.add_argument("--opencode-bin", default="opencode")
    parser.add_argument("--json", action="store_true", help="print JSON")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    db_path = Path(args.db).expanduser() if args.db else default_db_path()
    try:
        if db_path.is_file():
            connection = connect_readonly(db_path)
            try:
                result = measure(connection, args.session, args.title)
            finally:
                connection.close()
        else:
            if args.db:
                raise MeasurementError(f"OpenCode database not found: {db_path}")
            result = measure_via_export(args.opencode_bin, args.session, args.title)
    except MeasurementError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    if args.json:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print(format_human(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
