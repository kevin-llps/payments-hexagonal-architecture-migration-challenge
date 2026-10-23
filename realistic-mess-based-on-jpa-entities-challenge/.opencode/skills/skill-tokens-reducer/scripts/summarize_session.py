#!/usr/bin/env python3
"""Summarize one run session into compact redundancy signals.

Reads the recorded session messages (root and child sessions) and reports tool
call counts, repeated identical calls, repeated file reads, loaded skills and
error counts. This replaces reading a full transcript just to find waste.

Usage:
    summarize_session.py --session ses_... [--json]
    summarize_session.py --title "exact session title" [--json]
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).resolve().parent))
import measure_session_tokens as measurement  # noqa: E402

READ_TOOLS = {"read", "glob", "grep", "list", "webfetch"}


def stable(value: Any) -> str:
    try:
        return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(",", ":"))
    except (TypeError, ValueError):
        return str(value)


def excerpt(value: Any, limit: int = 180) -> str:
    text = stable(value)
    return text if len(text) <= limit else text[: limit - 1] + "…"


def load_messages(connection, session_ids: list[str]) -> list[dict[str, Any]]:
    if not session_ids:
        return []
    placeholders = ",".join("?" for _ in session_ids)
    rows = connection.execute(
        f"""
        SELECT session_id, type, data, seq
        FROM session_message
        WHERE session_id IN ({placeholders})
        ORDER BY time_created, seq
        """,
        session_ids,
    ).fetchall()
    messages: list[dict[str, Any]] = []
    for row in rows:
        try:
            data = json.loads(row["data"])
        except json.JSONDecodeError:
            continue
        messages.append({"session_id": row["session_id"], "type": row["type"], "data": data})
    return messages


def summarize(
    connection, session_id: str | None = None, title: str | None = None, limit: int = 20
) -> dict[str, Any]:
    table = measurement.choose_table(connection)
    root = measurement.find_root(connection, table, session_id, title)
    sessions = measurement.tree_rows(connection, table, root["id"])
    session_ids = [row["id"] for row in sessions]
    messages = load_messages(connection, session_ids)

    users = 0
    assistants = 0
    text_chars = 0
    reasoning_chars = 0
    result_chars = 0
    tool_errors = 0
    calls: Counter[tuple[str, str]] = Counter()
    reads: Counter[tuple[str, str]] = Counter()
    skills: list[str] = []
    by_name: Counter[str] = Counter()

    for message in messages:
        data = message["data"]
        if message["type"] == "user":
            users += 1
            text_chars += len(str(data.get("text") or ""))
            continue
        if message["type"] != "assistant":
            continue
        assistants += 1
        for part in data.get("content") or []:
            kind = part.get("type")
            if kind == "text":
                text_chars += len(part.get("text") or "")
            elif kind == "reasoning":
                reasoning_chars += len(part.get("text") or "")
            elif kind == "tool":
                name = str(part.get("name") or "unknown")
                by_name[name] += 1
                state = part.get("state") or {}
                payload = state.get("input")
                key = (name, stable(payload))
                calls[key] += 1
                if name in READ_TOOLS:
                    reads[key] += 1
                if name == "skill":
                    skill_id = None
                    if isinstance(payload, dict):
                        skill_id = payload.get("id") or payload.get("name")
                    if skill_id:
                        skills.append(str(skill_id))
                if state.get("status") in ("error", "failed"):
                    tool_errors += 1
                for item in state.get("content") or []:
                    if isinstance(item, dict) and item.get("type") == "text":
                        result_chars += len(item.get("text") or "")

    def repeated(counter: Counter[tuple[str, str]]) -> list[dict[str, Any]]:
        items = [
            {"name": name, "count": count, "input": excerpt(json.loads(payload))}
            for (name, payload), count in counter.items()
            if count > 1
        ]
        items.sort(key=lambda item: (-item["count"], item["name"], item["input"]))
        return items[:limit]

    return {
        "session_id": root["id"],
        "title": root["title"],
        "sessions": len(sessions),
        "messages": {"user": users, "assistant": assistants},
        "text_chars": text_chars,
        "reasoning_chars": reasoning_chars,
        "tools": {
            "total": sum(by_name.values()),
            "errors": tool_errors,
            "result_chars": result_chars,
            "by_name": dict(by_name.most_common()),
        },
        "repeated_calls": repeated(calls),
        "repeated_reads": repeated(reads),
        "skills_loaded": sorted(set(skills)),
    }


def format_human(result: dict[str, Any]) -> str:
    lines = [
        f"session: {result['session_id']}  ({result['sessions']} session(s))",
        f"messages: {result['messages']['user']} user / {result['messages']['assistant']} assistant",
        f"tools: {result['tools']['total']} calls, {result['tools']['errors']} errors, "
        f"{result['tools']['result_chars']:,} result chars",
        "tool histogram: "
        + ", ".join(
            f"{name}={count}" for name, count in result["tools"]["by_name"].items()
        ),
        f"skills loaded: {', '.join(result['skills_loaded']) or '-'}",
        "repeated calls:",
    ]
    if result["repeated_calls"]:
        for item in result["repeated_calls"]:
            lines.append(f"  {item['count']}x {item['name']} {item['input']}")
    else:
        lines.append("  none")
    lines.append("repeated reads:")
    if result["repeated_reads"]:
        for item in result["repeated_reads"]:
            lines.append(f"  {item['count']}x {item['name']} {item['input']}")
    else:
        lines.append("  none")
    return "\n".join(lines)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--session")
    source.add_argument("--title")
    parser.add_argument("--db")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--limit", type=int, default=20)
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    db_path = Path(args.db).expanduser() if args.db else measurement.default_db_path()
    try:
        connection = measurement.connect_readonly(db_path)
        try:
            result = summarize(connection, args.session, args.title, args.limit)
        finally:
            connection.close()
    except measurement.MeasurementError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    if args.json:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print(format_human(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
