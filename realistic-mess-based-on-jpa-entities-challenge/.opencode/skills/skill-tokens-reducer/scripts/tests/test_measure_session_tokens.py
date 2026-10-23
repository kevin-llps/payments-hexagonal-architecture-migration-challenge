"""Tests for measure_session_tokens.py.

Run with:
    python3 -m unittest discover -s .opencode/skills/skill-tokens-reducer/scripts/tests -v
"""

from __future__ import annotations

import importlib.util
import sqlite3
import tempfile
import unittest
from datetime import datetime
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
SCRIPT = SCRIPTS / "measure_session_tokens.py"

spec = importlib.util.spec_from_file_location("measure_session_tokens", SCRIPT)
measurement = importlib.util.module_from_spec(spec)
spec.loader.exec_module(measurement)

SCHEMA = """
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
)
"""


def stamp(day: int = 23, hour: int = 12) -> int:
    return int(datetime(2026, 10, day, hour).timestamp() * 1000)


def insert(
    connection: sqlite3.Connection,
    session_id: str,
    *,
    parent: str | None = None,
    title: str = "title",
    tokens: tuple[int, int, int, int, int] = (0, 0, 0, 0, 0),
    created: int | None = None,
) -> None:
    connection.execute(
        """
        INSERT INTO session_v2 (
            id, parent_id, title, directory, agent, model, cost,
            tokens_input, tokens_output, tokens_reasoning,
            tokens_cache_read, tokens_cache_write, time_created, time_updated
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            session_id,
            parent,
            title,
            "/tmp/project",
            "build",
            '{"id":"big-pickle","providerID":"opencode","variant":"default"}',
            0.01,
            *tokens,
            created or stamp(),
            created or stamp(),
        ),
    )


class MeasureTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.db = Path(self._tmp.name) / "opencode.db"
        self.connection = sqlite3.connect(self.db)
        self.connection.row_factory = sqlite3.Row
        self.connection.execute(SCHEMA)

    def tearDown(self):
        self.connection.close()
        self._tmp.cleanup()

    def test_total_includes_child_sessions(self):
        insert(self.connection, "ses_root", title="root", tokens=(100, 20, 10, 1000, 5))
        insert(
            self.connection,
            "ses_child",
            parent="ses_root",
            title="child",
            tokens=(50, 10, 0, 200, 0),
            created=stamp(hour=13),
        )
        self.connection.commit()

        result = measurement.measure(self.connection, session_id="ses_root")

        self.assertEqual(result["session_id"], "ses_root")
        self.assertEqual(result["children"], 1)
        self.assertEqual(len(result["sessions"]), 2)
        self.assertEqual(result["tokens"]["input"], 150)
        self.assertEqual(result["tokens"]["cache"]["read"], 1200)
        self.assertEqual(result["tokens"]["total"], 1395)
        self.assertEqual(result["model_label"], "opencode/big-pickle")
        self.assertEqual(result["date"], "2026-10-23")

    def test_title_resolution_is_exact(self):
        insert(self.connection, "ses_one", title="exact title", tokens=(1, 2, 3, 4, 5))
        insert(self.connection, "ses_two", title="other", tokens=(0, 0, 0, 0, 0))
        self.connection.commit()

        result = measurement.measure(self.connection, title="exact title")

        self.assertEqual(result["session_id"], "ses_one")
        self.assertEqual(result["tokens"]["total"], 15)

    def test_duplicate_title_is_an_error(self):
        insert(self.connection, "ses_one", title="same")
        insert(self.connection, "ses_two", title="same")
        self.connection.commit()

        with self.assertRaises(measurement.MeasurementError):
            measurement.measure(self.connection, title="same")

    def test_missing_session_is_an_error(self):
        with self.assertRaises(measurement.MeasurementError):
            measurement.measure(self.connection, session_id="ses_missing")

    def test_missing_database_is_an_error(self):
        with self.assertRaises(measurement.MeasurementError):
            measurement.connect_readonly(Path(self._tmp.name) / "absent.db")

    def test_human_output_contains_total(self):
        insert(self.connection, "ses_root", tokens=(1, 2, 3, 4, 5))
        self.connection.commit()
        result = measurement.measure(self.connection, session_id="ses_root")
        human = measurement.format_human(result)
        self.assertIn("total=15", human)
        self.assertIn("ses_root", human)


if __name__ == "__main__":
    unittest.main()
