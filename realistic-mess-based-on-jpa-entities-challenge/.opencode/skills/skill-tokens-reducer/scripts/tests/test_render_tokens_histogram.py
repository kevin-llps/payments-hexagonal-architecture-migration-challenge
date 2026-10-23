"""Tests for render_tokens_histogram.py."""

from __future__ import annotations

import importlib.util
import io
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
SCRIPT = SCRIPTS / "render_tokens_histogram.py"

spec = importlib.util.spec_from_file_location("render_tokens_histogram", SCRIPT)
histogram = importlib.util.module_from_spec(spec)
spec.loader.exec_module(histogram)

ROW_A = (
    "| 2026-10-23 | `ses_a` | 2 | baseline | nominal | 1 | title a | reason a | "
    "opencode/big-pickle#default / build | 10 | 20 | 30 | 40 | 50 | 150 | PASS |"
)
ROW_B = (
    "| 2026-10-24 | `ses_b` | 0 | opt-1 | edge\\|case | 2 | title b | reason b | "
    "opencode/big-pickle#default / build | 10 | 20 | 30 | 40 | 50 | 3,000 | PASS |"
)


class HistoryTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.path = Path(self._tmp.name) / "TOKENS_HISTORY.md"
        self.path.write_text(histogram.new_history("demo"), encoding="utf-8")

    def tearDown(self):
        self._tmp.cleanup()

    def add_rows(self, rows):
        lines = self.path.read_text(encoding="utf-8").splitlines()
        _, _, index = histogram.find_runs_table(lines)
        for row in rows:
            lines.insert(index, row)
            index += 1
        self.path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    def test_new_history_has_markers_and_empty_block(self):
        text = self.path.read_text(encoding="utf-8")
        self.assertIn(histogram.START, text)
        self.assertIn(histogram.END, text)
        self.assertIn("Total sessions: 0", text)

    def test_rewrite_replaces_only_marked_block(self):
        self.add_rows([ROW_A, ROW_B])
        before = self.path.read_text(encoding="utf-8")
        rows = histogram.rewrite_history(self.path)
        after = self.path.read_text(encoding="utf-8")
        self.assertEqual(len(rows), 2)
        self.assertEqual(before.split(histogram.START)[0], after.split(histogram.START)[0])
        self.assertEqual(after.count(histogram.START), 1)
        self.assertEqual(after.count(histogram.END), 1)
        self.assertIn("ses_a", after)
        self.assertIn("ses_b", after)
        self.assertIn("3,000", after)

    def test_escaped_pipe_is_unescaped(self):
        self.add_rows([ROW_B])
        rows = histogram.parse_runs(self.path.read_text(encoding="utf-8"))
        self.assertEqual(rows[0]["scenario"], "edge|case")
        self.assertEqual(rows[0]["total"], 3000)

    def test_histogram_scale_and_bars(self):
        self.add_rows([ROW_A, ROW_B])
        rows = histogram.rewrite_history(self.path)
        lines = histogram.render_rows(rows, width=10)
        self.assertIn("1 block = 300 tokens", "\n".join(lines))
        self.assertIn("██████████", "\n".join(lines))

    def test_filters_do_not_edit_file(self):
        self.add_rows([ROW_A, ROW_B])
        histogram.rewrite_history(self.path)
        text = self.path.read_text(encoding="utf-8")
        rows = histogram.parse_runs(text)
        filtered = histogram.filter_rows(rows, last=1)
        self.assertEqual([row["session_id"] for row in filtered], ["ses_b"])
        self.assertEqual(histogram.filter_rows(rows, since="2026-10-24"), filtered)
        self.assertEqual(histogram.filter_rows(rows, phase="baseline")[0]["session_id"], "ses_a")
        self.assertEqual(self.path.read_text(encoding="utf-8"), text)

    def test_print_only_does_not_rewrite_the_file(self):
        self.add_rows([ROW_A])
        histogram.rewrite_history(self.path)
        sentinel = self.path.read_text(encoding="utf-8").replace(
            "Total sessions: 1", "SENTINEL"
        )
        self.path.write_text(sentinel, encoding="utf-8")
        with redirect_stdout(io.StringIO()):
            code = histogram.main(["--history", str(self.path), "--print-only"])
        self.assertEqual(code, 0)
        self.assertIn("SENTINEL", self.path.read_text(encoding="utf-8"))

    def test_missing_history_is_an_error(self):
        self.path.unlink()
        with self.assertRaises(histogram.HistoryError):
            histogram.rewrite_history(self.path)

    def test_unbalanced_markers_are_an_error(self):
        text = self.path.read_text(encoding="utf-8").replace(histogram.END, "")
        self.path.write_text(text, encoding="utf-8")
        with self.assertRaises(histogram.HistoryError):
            histogram.rewrite_history(self.path)


if __name__ == "__main__":
    unittest.main()
