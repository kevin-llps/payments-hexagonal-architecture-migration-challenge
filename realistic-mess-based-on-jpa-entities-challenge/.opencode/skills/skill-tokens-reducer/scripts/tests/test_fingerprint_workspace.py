"""Tests for fingerprint_workspace.py."""

from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
SCRIPT = SCRIPTS / "fingerprint_workspace.py"

spec = importlib.util.spec_from_file_location("fingerprint_workspace", SCRIPT)
fingerprint = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fingerprint)


class FingerprintTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name)
        self.skill = self.root / "demo"
        self.skill.mkdir()
        (self.skill / "SKILL.md").write_text("# Demo\n", encoding="utf-8")
        (self.skill / "references").mkdir()
        (self.skill / "references" / "a.md").write_text("a\n", encoding="utf-8")
        (self.skill / "TOKENS_HISTORY.md").write_text("history\n", encoding="utf-8")

    def tearDown(self):
        self._tmp.cleanup()

    def test_hash_tree_is_stable_and_excludes_git(self):
        (self.skill / ".git").mkdir()
        (self.skill / ".git" / "config").write_text("x\n", encoding="utf-8")
        first = fingerprint.hash_tree(self.skill)
        second = fingerprint.hash_tree(self.skill)
        self.assertEqual(first, second)
        self.assertIn("SKILL.md", first["files"])
        self.assertNotIn(".git/config", first["files"])

    def test_compare_reports_added_removed_changed(self):
        before = fingerprint.hash_tree(self.skill)
        (self.skill / "SKILL.md").write_text("# Changed\n", encoding="utf-8")
        (self.skill / "references" / "a.md").unlink()
        (self.skill / "new.md").write_text("new\n", encoding="utf-8")
        after = fingerprint.hash_tree(self.skill)
        result = fingerprint.compare_manifests(before, after)
        self.assertFalse(result["equal"])
        self.assertEqual(result["changed"], ["SKILL.md"])
        self.assertEqual(result["removed"], ["references/a.md"])
        self.assertEqual(result["added"], ["new.md"])

    def test_snapshot_and_restore_preserve_history(self):
        snapshot_dir = self.root / "cycle"
        fingerprint.snapshot(self.skill, snapshot_dir)
        (self.skill / "SKILL.md").write_text("# Optimized\n", encoding="utf-8")
        (self.skill / "new.md").write_text("temporary\n", encoding="utf-8")
        (self.skill / "TOKENS_HISTORY.md").write_text("updated history\n", encoding="utf-8")

        fingerprint.restore(snapshot_dir, self.skill)

        self.assertEqual((self.skill / "SKILL.md").read_text(encoding="utf-8"), "# Demo\n")
        self.assertFalse((self.skill / "new.md").exists())
        self.assertEqual(
            (self.skill / "TOKENS_HISTORY.md").read_text(encoding="utf-8"),
            "updated history\n",
        )

    def test_snapshot_refuses_existing_target_without_force(self):
        snapshot_dir = self.root / "cycle"
        snapshot_dir.mkdir()
        with self.assertRaises(fingerprint.FingerprintError):
            fingerprint.snapshot(self.skill, snapshot_dir)

    def test_restore_rejects_missing_snapshot(self):
        with self.assertRaises(fingerprint.FingerprintError):
            fingerprint.restore(self.root / "absent", self.skill)

    def test_manifest_round_trip(self):
        manifest_path = self.root / "manifest.json"
        manifest_path.write_text(
            json.dumps(fingerprint.hash_tree(self.skill)), encoding="utf-8"
        )
        self.assertEqual(
            fingerprint.read_manifest(manifest_path)["root"], str(self.skill.resolve())
        )


if __name__ == "__main__":
    unittest.main()
