"""Tests for append_tokens_history.py."""

from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
SCRIPT = SCRIPTS / "append_tokens_history.py"

spec = importlib.util.spec_from_file_location("append_tokens_history", SCRIPT)
append = importlib.util.module_from_spec(spec)
spec.loader.exec_module(append)


def sample_run(session_id: str = "ses_append") -> dict:
    return {
        "date": "2026-10-23",
        "session_id": session_id,
        "children": 1,
        "phase": "baseline",
        "scenario": "nominal|edge",
        "run": 1,
        "title": "tokens:demo:nominal:baseline:1:abc",
        "reason": "why | because",
        "model": "opencode/big-pickle#default",
        "agent": "build",
        "tokens": {
            "input": 10,
            "output": 20,
            "reasoning": 30,
            "cache": {"read": 40, "write": 50},
            "total": 150,
        },
        "result": "PASS",
    }


class AppendTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.history = Path(self._tmp.name) / "TOKENS_HISTORY.md"

    def tearDown(self):
        self._tmp.cleanup()

    def test_missing_history_is_created_when_target_is_given(self):
        self.assertTrue(append.append_run(self.history, sample_run(), target="demo"))
        self.assertTrue(self.history.is_file())
        self.assertIn("ses_append", self.history.read_text(encoding="utf-8"))

    def test_appended_row_is_visible_and_histogram_updates(self):
        append.append_run(self.history, sample_run(), target="demo")
        text = self.history.read_text(encoding="utf-8")
        self.assertIn("| 2026-10-23 | `ses_append` | 1 | baseline |", text)
        self.assertIn("| 150 |", text)
        self.assertIn("Total sessions: 1", text)
        self.assertIn("nominal\\|edge", text)
        self.assertIn("why \\| because", text)

    def test_duplicate_session_is_not_appended_twice(self):
        self.assertTrue(append.append_run(self.history, sample_run(), target="demo"))
        self.assertFalse(append.append_run(self.history, sample_run(), target="demo"))
        text = self.history.read_text(encoding="utf-8")
        self.assertEqual(text.count("| 2026-10-23 | `ses_append` | 1 | baseline |"), 1)

    def test_second_session_appends_after_the_first(self):
        append.append_run(self.history, sample_run("ses_one"), target="demo")
        append.append_run(self.history, sample_run("ses_two"), target="demo")
        text = self.history.read_text(encoding="utf-8")
        self.assertLess(text.index("`ses_one`"), text.index("`ses_two`"))

    def test_missing_required_field_is_an_error(self):
        run = sample_run()
        del run["result"]
        with self.assertRaises(append.AppendError):
            append.append_run(self.history, run, target="demo")

    def test_missing_history_without_target_is_an_error(self):
        with self.assertRaises(append.AppendError):
            append.append_run(self.history, sample_run())

    def test_token_total_falls_back_to_components(self):
        run = sample_run()
        run["tokens"].pop("total")
        self.assertEqual(append.token_total(run), 150)


if __name__ == "__main__":
    unittest.main()
