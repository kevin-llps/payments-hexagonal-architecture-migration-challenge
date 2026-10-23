"""Tests for summarize_session.py."""

from __future__ import annotations

import importlib.util
import json
import sqlite3
import tempfile
import unittest
from datetime import datetime
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
SCRIPT = SCRIPTS / "summarize_session.py"

spec = importlib.util.spec_from_file_location("summarize_session", SCRIPT)
summary = importlib.util.module_from_spec(spec)
spec.loader.exec_module(summary)

SESSION_SCHEMA = """
CREATE TABLE session_v2 (
    id TEXT PRIMARY KEY,
    parent_id TEXT,
    title TEXT,
    directory TEXT,
    agent TEXT,
    model TEXT,
    cost REAL,
    tokens_input INTEGER DEFAULT 0,
    tokens_output INTEGER DEFAULT 0,
    tokens_reasoning INTEGER DEFAULT 0,
    tokens_cache_read INTEGER DEFAULT 0,
    tokens_cache_write INTEGER DEFAULT 0,
    time_created INTEGER,
    time_updated INTEGER
);
CREATE TABLE session_message (
    id TEXT PRIMARY KEY,
    session_id TEXT,
    type TEXT,
    data TEXT,
    seq INTEGER,
    time_created INTEGER
);
"""


def stamp(hour: int = 12) -> int:
    return int(datetime(2026, 10, 23, hour).timestamp() * 1000)


def add_session(connection, session_id: str, parent: str | None = None) -> None:
    connection.execute(
        """
        INSERT INTO session_v2 (
            id, parent_id, title, directory, agent, model, cost,
            time_created, time_updated
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (session_id, parent, session_id, "/tmp", "build", "opencode/big-pickle", 0, stamp(), stamp()),
    )


def add_message(connection, message_id: str, session_id: str, kind: str, data: dict, seq: int) -> None:
    connection.execute(
        "INSERT INTO session_message VALUES (?, ?, ?, ?, ?, ?)",
        (message_id, session_id, kind, json.dumps(data), seq, stamp()),
    )


class SummaryTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.connection = sqlite3.connect(Path(self._tmp.name) / "db.sqlite")
        self.connection.row_factory = sqlite3.Row
        self.connection.executescript(SESSION_SCHEMA)
        add_session(self.connection, "ses_root")
        add_session(self.connection, "ses_child", parent="ses_root")
        add_message(
            self.connection,
            "m1",
            "ses_root",
            "user",
            {"text": "please measure"},
            1,
        )
        add_message(
            self.connection,
            "m2",
            "ses_root",
            "assistant",
            {
                "content": [
                    {"type": "text", "text": "hello"},
                    {"type": "reasoning", "text": "think"},
                    {
                        "type": "tool",
                        "name": "read",
                        "state": {
                            "status": "completed",
                            "input": {"path": "a.md"},
                            "content": [{"type": "text", "text": "abc"}],
                        },
                    },
                    {
                        "type": "tool",
                        "name": "read",
                        "state": {
                            "status": "completed",
                            "input": {"path": "a.md"},
                            "content": [{"type": "text", "text": "abc"}],
                        },
                    },
                    {
                        "type": "tool",
                        "name": "skill",
                        "state": {
                            "status": "completed",
                            "input": {"id": "demo"},
                            "content": [],
                        },
                    },
                ]
            },
            2,
        )
        add_message(
            self.connection,
            "m3",
            "ses_child",
            "assistant",
            {
                "content": [
                    {
                        "type": "tool",
                        "name": "shell",
                        "state": {
                            "status": "error",
                            "input": {"command": "false"},
                            "content": [{"type": "text", "text": "oops"}],
                        },
                    }
                ]
            },
            3,
        )
        self.connection.commit()

    def tearDown(self):
        self.connection.close()
        self._tmp.cleanup()

    def test_counts_include_child_sessions(self):
        result = summary.summarize(self.connection, session_id="ses_root")
        self.assertEqual(result["sessions"], 2)
        self.assertEqual(result["messages"], {"user": 1, "assistant": 2})
        self.assertEqual(result["tools"]["total"], 4)
        self.assertEqual(result["tools"]["by_name"]["read"], 2)
        self.assertEqual(result["tools"]["by_name"]["shell"], 1)
        self.assertEqual(result["tools"]["errors"], 1)
        self.assertEqual(result["skills_loaded"], ["demo"])
        self.assertEqual(result["reasoning_chars"], len("think"))
        self.assertEqual(result["tools"]["result_chars"], len("abc") * 2 + len("oops"))

    def test_repeated_calls_and_reads_are_reported(self):
        result = summary.summarize(self.connection, session_id="ses_root")
        self.assertEqual(len(result["repeated_calls"]), 1)
        self.assertEqual(result["repeated_calls"][0]["name"], "read")
        self.assertEqual(result["repeated_calls"][0]["count"], 2)
        self.assertEqual(result["repeated_reads"][0]["count"], 2)

    def test_title_resolution(self):
        result = summary.summarize(self.connection, title="ses_root")
        self.assertEqual(result["session_id"], "ses_root")

    def test_human_output_mentions_redundancy(self):
        result = summary.summarize(self.connection, session_id="ses_root")
        human = summary.format_human(result)
        self.assertIn("2x read", human)
        self.assertIn("skills loaded: demo", human)


if __name__ == "__main__":
    unittest.main()
