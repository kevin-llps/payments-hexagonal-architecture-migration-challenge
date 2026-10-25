"""CLI tests for enforce-new-skill-scope.py.

Run with:
    python3 -m unittest discover -s .opencode/skills/skill-testing/scripts/tests -v

Each test points TMPDIR at its own directory so the baseline snapshot cannot
touch the real one.
"""

from __future__ import annotations

import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
SCRIPT = SCRIPTS / "enforce-new-skill-scope.py"


class GuardCliTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.tmp = Path(self._tmp.name)
        self.tmpdir = self.tmp / "tmp"
        self.tmpdir.mkdir()
        self.root = self.tmp / "skills"
        (self.root / "existing").mkdir(parents=True)
        (self.root / "existing" / "SKILL.md").write_text("existing\n", encoding="utf-8")

    def tearDown(self):
        self._tmp.cleanup()

    def run_cli(self, *args: str) -> subprocess.CompletedProcess:
        env = {**os.environ, "TMPDIR": str(self.tmpdir)}
        return subprocess.run(
            [sys.executable, str(SCRIPT), *args],
            capture_output=True,
            text=True,
            env=env,
        )

    def snapshot(self):
        result = self.run_cli("snapshot", "--root", str(self.root))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def write(self, relative: str, text: str = "file\n") -> None:
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def test_new_skill_confined_is_clean(self):
        self.snapshot()
        self.write("new-skill/SKILL.md", "new\n")
        result = self.run_cli("check", "--skill", "new-skill", "--root", str(self.root))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("Result: CLEAN", result.stdout)

    def test_modifying_existing_skill_is_a_violation(self):
        self.snapshot()
        self.write("existing/SKILL.md", "tampered\n")
        result = self.run_cli("check", "--skill", "new-skill", "--root", str(self.root))
        self.assertEqual(result.returncode, 1)
        self.assertIn("modified: existing/SKILL.md", result.stdout)
        self.assertIn("Result: VIOLATION", result.stdout)

    def test_deleting_existing_file_is_a_violation(self):
        self.snapshot()
        (self.root / "existing" / "SKILL.md").unlink()
        result = self.run_cli("check", "--skill", "new-skill", "--root", str(self.root))
        self.assertEqual(result.returncode, 1)
        self.assertIn("deleted: existing/SKILL.md", result.stdout)

    def test_new_file_outside_new_skill_is_a_violation(self):
        self.snapshot()
        self.write("existing/stray.txt")
        result = self.run_cli("check", "--skill", "new-skill", "--root", str(self.root))
        self.assertEqual(result.returncode, 1)
        self.assertIn("created outside new-skill/: existing/stray.txt", result.stdout)

    def test_new_file_nested_in_new_skill_is_clean(self):
        self.snapshot()
        self.write("new-skill/tests/rules.json", "{}\n")
        result = self.run_cli("check", "--skill", "new-skill", "--root", str(self.root))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_check_without_snapshot_exits_two(self):
        result = self.run_cli("check", "--skill", "new-skill", "--root", str(self.root))
        self.assertEqual(result.returncode, 2)
        self.assertIn("no snapshot", result.stderr)

    def test_snapshot_root_mismatch_exits_two(self):
        self.snapshot()
        other = self.tmp / "other"
        other.mkdir()
        result = self.run_cli("check", "--skill", "new-skill", "--root", str(other))
        self.assertEqual(result.returncode, 2)
        self.assertIn("does not match", result.stderr)

    def test_invalid_skill_id_exits_two(self):
        result = self.run_cli("check", "--skill", "Bad_ID", "--root", str(self.root))
        self.assertEqual(result.returncode, 2)
        self.assertIn("invalid skill id", result.stderr)

    def test_missing_root_exits_two(self):
        result = self.run_cli("snapshot", "--root", str(self.tmp / "absent"))
        self.assertEqual(result.returncode, 2)
        self.assertIn("root does not exist", result.stderr)

    def test_usage_error_without_command_exits_two(self):
        result = self.run_cli()
        self.assertEqual(result.returncode, 2)
        self.assertIn("usage:", result.stderr)


if __name__ == "__main__":
    unittest.main()
