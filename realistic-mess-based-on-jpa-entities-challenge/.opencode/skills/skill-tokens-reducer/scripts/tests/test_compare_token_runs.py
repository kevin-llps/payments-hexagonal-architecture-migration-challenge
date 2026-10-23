"""Tests for compare_token_runs.py."""

from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
SCRIPT = SCRIPTS / "compare_token_runs.py"

spec = importlib.util.spec_from_file_location("compare_token_runs", SCRIPT)
comparison = importlib.util.module_from_spec(spec)
spec.loader.exec_module(comparison)


def make_run(scenario: str, run: int, total: int, result: str = "PASS", **extra) -> dict:
    data = {
        "scenario": scenario,
        "scenario_hash": "scenario-hash",
        "run": run,
        "result": result,
        "tokens": {"total": total},
        "target_tests_hash": {"rules.json": "abc"},
        "artifact_hashes": {"out.txt": "same"},
        "model": "opencode/big-pickle#default",
        "agent": "build",
        "target": "demo",
    }
    data.update(extra)
    return data


class CompareTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name)

    def tearDown(self):
        self._tmp.cleanup()

    def write_phase(self, name: str, runs: list[dict]) -> Path:
        directory = self.root / name
        for run in runs:
            path = directory / run["scenario"] / f"run-{run['run']}.json"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(run), encoding="utf-8")
        return directory

    def test_accept_requires_every_scenario_to_drop_by_threshold(self):
        baseline = [make_run("nominal", 1, 1000), make_run("edge", 1, 2000)]
        candidate = [make_run("nominal", 1, 900), make_run("edge", 1, 1800)]
        result = comparison.compare(baseline, candidate, 5)
        self.assertEqual(result["verdict"], "ACCEPT")
        self.assertTrue(result["results_ok"])
        self.assertEqual(result["scenarios"][0]["delta_percent"], 10.0)

    def test_small_gain_is_inconclusive(self):
        baseline = [make_run("nominal", 1, 1000)]
        candidate = [make_run("nominal", 1, 990)]
        result = comparison.compare(baseline, candidate, 5)
        self.assertEqual(result["verdict"], "INCONCLUSIVE")

    def test_regression_is_rejected(self):
        baseline = [make_run("nominal", 1, 1000)]
        candidate = [make_run("nominal", 1, 1200)]
        result = comparison.compare(baseline, candidate, 5)
        self.assertEqual(result["verdict"], "REJECT")

    def test_failed_result_is_rejected_even_with_gain(self):
        baseline = [make_run("nominal", 1, 1000)]
        candidate = [make_run("nominal", 1, 500, result="FAIL")]
        result = comparison.compare(baseline, candidate, 5)
        self.assertEqual(result["verdict"], "REJECT")
        self.assertFalse(result["results_ok"])

    def test_artifact_change_is_rejected(self):
        baseline = [make_run("nominal", 1, 1000)]
        candidate = [make_run("nominal", 1, 900, artifact_hashes={"out.txt": "changed"})]
        result = comparison.compare(baseline, candidate, 5)
        self.assertEqual(result["verdict"], "REJECT")
        self.assertFalse(result["artifacts_ok"])

    def test_target_tests_change_is_rejected(self):
        baseline = [make_run("nominal", 1, 1000)]
        candidate = [make_run("nominal", 1, 900, target_tests_hash={"rules.json": "changed"})]
        result = comparison.compare(baseline, candidate, 5)
        self.assertEqual(result["verdict"], "REJECT")
        self.assertFalse(result["tests_ok"])

    def test_model_change_is_rejected(self):
        baseline = [make_run("nominal", 1, 1000)]
        candidate = [make_run("nominal", 1, 900, model="other/model")]
        result = comparison.compare(baseline, candidate, 5)
        self.assertEqual(result["verdict"], "REJECT")
        self.assertFalse(result["settings_ok"])

    def test_scenario_file_change_is_rejected(self):
        baseline = [make_run("nominal", 1, 1000)]
        candidate = [make_run("nominal", 1, 900, scenario_hash="different")]
        result = comparison.compare(baseline, candidate, 5)
        self.assertEqual(result["verdict"], "REJECT")
        self.assertFalse(result["settings_ok"])

    def test_mismatched_run_counts_are_an_error(self):
        baseline = [make_run("nominal", 1, 1000)]
        candidate = [make_run("nominal", 1, 900), make_run("nominal", 2, 900)]
        with self.assertRaises(comparison.CompareError):
            comparison.compare(baseline, candidate, 5)

    def test_load_runs_reads_nested_directories(self):
        directory = self.write_phase("phase", [make_run("nominal", 1, 1000)])
        runs = comparison.load_runs(directory)
        self.assertEqual(len(runs), 1)
        self.assertEqual(comparison.total_of(runs[0]), 1000)

    def test_missing_directory_is_an_error(self):
        with self.assertRaises(comparison.CompareError):
            comparison.load_runs(self.root / "absent")


if __name__ == "__main__":
    unittest.main()
