#!/usr/bin/env python3
"""Render the per-session token histogram inside TOKENS_HISTORY.md.

The run table is append-only and is never touched. Only the lines between the
TOKENS_HISTOGRAM markers are replaced, and the block is derived from the
recorded run rows.

Usage:
    render_tokens_histogram.py --history .opencode/skills/<target>/TOKENS_HISTORY.md
    render_tokens_histogram.py --history ... --print [--last N] [--since DATE]
"""

from __future__ import annotations

import argparse
import math
import os
import re
import sys
from pathlib import Path
from typing import Any

START = "<!-- TOKENS_HISTOGRAM:START -->"
END = "<!-- TOKENS_HISTOGRAM:END -->"
HISTORY_TITLE = "## Token consumption per session"
RUNS_TITLE = "## Runs"
DEFAULT_WIDTH = 40

RUN_HEADER = (
    "| Date | Session ID | Children | Phase | Scenario | Run | Session title | "
    "Why invoked | Model / agent | Input | Output | Reasoning | Cache read | "
    "Cache write | Total | Result |"
)
RUN_SEPARATOR = (
    "| --- | --- | ---: | --- | --- | ---: | --- | --- | --- | ---: | ---: | "
    "---: | ---: | ---: | ---: | --- |"
)


class HistoryError(RuntimeError):
    """Raised when the history file cannot be parsed or rewritten."""


def new_history(target: str) -> str:
    rows = render_rows([], DEFAULT_WIDTH)
    return (
        f"# Token history - {target}\n\n"
        f"{RUNS_TITLE}\n\n"
        f"{RUN_HEADER}\n{RUN_SEPARATOR}\n\n"
        f"{HISTORY_TITLE}\n\n"
        f"{START}\n"
        + "\n".join(rows)
        + f"\n{END}\n"
    )


def split_cells(line: str) -> list[str]:
    stripped = line.strip()
    if not stripped.startswith("|"):
        return []
    body = stripped[1:-1] if stripped.endswith("|") else stripped[1:]
    return [cell.strip().replace("\\|", "|") for cell in re.split(r"(?<!\\)\|", body)]


def find_runs_table(lines: list[str]) -> tuple[list[str], list[list[str]], int]:
    heading = next(
        (index for index, line in enumerate(lines) if line.strip().lower() == RUNS_TITLE.lower()),
        None,
    )
    if heading is None:
        raise HistoryError(f'heading not found: "{RUNS_TITLE}"')
    start = next(
        (index for index in range(heading + 1, len(lines)) if lines[index].strip().startswith("|")),
        None,
    )
    if start is None:
        raise HistoryError("run table not found under ## Runs")
    header = split_cells(lines[start])
    rows: list[list[str]] = []
    index = start + 1
    if index < len(lines) and set(lines[index].replace("|", "").replace(":", "").strip()) <= {"-", " "}:
        index += 1
    while index < len(lines) and lines[index].strip().startswith("|"):
        rows.append(split_cells(lines[index]))
        index += 1
    return header, rows, index


def column_index(header: list[str], *names: str) -> int | None:
    normalized = [re.sub(r"[`*_]", "", cell).strip().lower() for cell in header]
    for name in names:
        if name in normalized:
            return normalized.index(name)
    return None


def parse_int(raw: str) -> int:
    cleaned = re.sub(r"[`*\s,]", "", raw or "")
    if cleaned in ("", "-", "—"):
        return 0
    try:
        return int(float(cleaned))
    except ValueError as error:
        raise HistoryError(f"invalid integer cell: {raw!r}") from error


def parse_runs(text: str) -> list[dict[str, Any]]:
    lines = text.splitlines()
    header, rows, _ = find_runs_table(lines)
    indexes = {
        "date": column_index(header, "date"),
        "session": column_index(header, "session id", "session"),
        "children": column_index(header, "children", "child sessions"),
        "phase": column_index(header, "phase"),
        "scenario": column_index(header, "scenario"),
        "run": column_index(header, "run"),
        "total": column_index(header, "total", "total tokens"),
    }
    if indexes["session"] is None or indexes["total"] is None:
        raise HistoryError("run table needs Session ID and Total columns")
    parsed: list[dict[str, Any]] = []
    for row in rows:
        if len(row) <= max(indexes["session"], indexes["total"]):
            continue
        session = row[indexes["session"]].strip("` ")
        if not session.startswith("ses_"):
            continue

        def cell(name: str) -> str:
            index = indexes[name]
            if index is None or index >= len(row):
                return ""
            return row[index]

        parsed.append(
            {
                "date": cell("date"),
                "session_id": session,
                "children": parse_int(cell("children")),
                "phase": cell("phase"),
                "scenario": cell("scenario"),
                "run": cell("run"),
                "total": parse_int(cell("total")),
            }
        )
    return parsed


def render_rows(rows: list[dict[str, Any]], width: int = DEFAULT_WIDTH) -> list[str]:
    maximum = max((row["total"] for row in rows), default=0)
    unit = max(1, math.ceil(maximum / width)) if maximum else 1
    lines = [
        "| Date | Session ID | Phase | Scenario | Run | Children | Total tokens | Histogram |",
        "| --- | --- | --- | --- | ---: | ---: | ---: | --- |",
    ]
    for row in rows:
        total = int(row["total"])
        blocks = 0 if total <= 0 else min(width, max(1, math.ceil(total / unit)))
        lines.append(
            "| {date} | `{session}` | {phase} | {scenario} | {run} | {children} | {total:,} | {bar} |".format(
                date=row["date"],
                session=row["session_id"],
                phase=row["phase"],
                scenario=row["scenario"],
                run=row["run"],
                children=row["children"],
                total=total,
                bar="█" * blocks,
            )
        )
    lines.append("")
    lines.append(
        f"Total sessions: {len(rows)} · max: {maximum:,} · "
        f"scale: 1 block = {unit:,} token{'s' if unit != 1 else ''}"
    )
    return lines


def filter_rows(
    rows: list[dict[str, Any]],
    last: int | None = None,
    since: str | None = None,
    phase: str | None = None,
) -> list[dict[str, Any]]:
    filtered = rows
    if since:
        filtered = [row for row in filtered if (row["date"] or "") >= since]
    if phase:
        filtered = [row for row in filtered if row["phase"] == phase]
    if last is not None and last >= 0:
        filtered = filtered[-last:] if last else []
    return filtered


def replace_block(text: str, block_lines: list[str]) -> str:
    lines = text.splitlines()
    starts = [index for index, line in enumerate(lines) if line.strip() == START]
    ends = [index for index, line in enumerate(lines) if line.strip() == END]
    if starts and ends and starts[0] < ends[0]:
        new_lines = lines[: starts[0]] + block_lines + lines[ends[0] + 1 :]
        return "\n".join(new_lines).rstrip() + "\n"
    if starts or ends:
        raise HistoryError("histogram markers are unbalanced")
    suffix = "" if text.endswith("\n") else "\n"
    return (
        text
        + suffix
        + "\n"
        + HISTORY_TITLE
        + "\n\n"
        + "\n".join(block_lines)
        + "\n"
    )


def rewrite_history(path: Path, width: int = DEFAULT_WIDTH) -> list[dict[str, Any]]:
    if not path.is_file():
        raise HistoryError(f"history not found: {path}")
    text = path.read_text(encoding="utf-8")
    rows = parse_runs(text)
    block = [START, *render_rows(rows, width), END]
    updated = replace_block(text, block)
    temporary = path.with_name(f".{path.name}.tmp")
    temporary.write_text(updated, encoding="utf-8")
    os.replace(temporary, path)
    return rows


def print_rows(rows: list[dict[str, Any]], width: int = DEFAULT_WIDTH) -> None:
    print(HISTORY_TITLE.lstrip("# ").strip())
    print("\n".join(render_rows(rows, width)))


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--history", required=True, help="path to TOKENS_HISTORY.md")
    parser.add_argument("--width", type=int, default=DEFAULT_WIDTH)
    parser.add_argument("--print", action="store_true", help="print the histogram")
    parser.add_argument("--print-only", action="store_true", help="do not rewrite the file")
    parser.add_argument("--last", type=int, help="print only the last N sessions")
    parser.add_argument("--since", help="print sessions from this date (YYYY-MM-DD)")
    parser.add_argument("--phase", help="print only this phase")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    path = Path(args.history).expanduser()
    if args.width < 1:
        print("error: --width must be positive", file=sys.stderr)
        return 2
    try:
        if args.print_only:
            if not path.is_file():
                raise HistoryError(f"history not found: {path}")
            rows = parse_runs(path.read_text(encoding="utf-8"))
        else:
            rows = rewrite_history(path, args.width)
    except HistoryError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    if args.print or args.print_only:
        print_rows(filter_rows(rows, args.last, args.since, args.phase), args.width)
    else:
        print(f"history: {path}")
        print(f"sessions: {len(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
