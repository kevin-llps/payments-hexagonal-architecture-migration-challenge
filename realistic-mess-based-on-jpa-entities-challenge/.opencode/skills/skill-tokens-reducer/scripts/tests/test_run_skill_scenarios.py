"""Tests for run_skill_scenarios.py pure helpers and orchestration."""

from __future__ import annotations

import contextlib
import importlib.util
import io
import json
import os
import sqlite3
import sys
import tempfile
import time
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
SCRIPT = SCRIPTS / "run_skill_scenarios.py"

spec = importlib.util.spec_from_file_location("run_skill_scenarios", SCRIPT)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class HelperTests(unittest.TestCase):
    def test_build_command_pins_format_agent_model_and_title(self):
        command = runner.build_command(
            "opencode", "build", "opencode/big-pickle#default", "title", "prompt"
        )
        self.assertEqual(command[:2], ["opencode", "run"])
        self.assertIn("--format", command)
        self.assertEqual(command[command.index("--agent") + 1], "build")
        self.assertEqual(
            command[command.index("--model") + 1], "opencode/big-pickle#default"
        )
        self.assertEqual(command[command.index("--title") + 1], "title")
        self.assertIn("--auto", command)
        self.assertIn("--standalone", command)
        self.assertEqual(command[-1], "prompt")

    def test_sandbox_env_points_pwd_at_the_sandbox(self):
        sandbox = Path("/tmp/some sandbox")
        environment = runner.sandbox_env(sandbox)
        self.assertEqual(environment["PWD"], str(sandbox))
        self.assertEqual(environment["OLDPWD"], str(sandbox))
        self.assertIn("PATH", environment)

    def test_parse_events_captures_session_and_text(self):
        stdout = "\n".join(
            [
                json.dumps({"type": "step_start", "sessionID": "ses_abc"}),
                json.dumps({"type": "text", "part": {"text": "first"}}),
                "not json",
                json.dumps({"type": "text", "part": {"text": "second"}}),
                json.dumps({"type": "error", "error": "boom"}),
            ]
        )
        result = runner.parse_events(stdout)
        self.assertEqual(result["session_id"], "ses_abc")
        self.assertEqual(result["text"], "first\nsecond")
        self.assertEqual(result["errors"], ["boom"])

    def test_phase_and_title_are_safe_and_unique(self):
        self.assertEqual(runner.sanitize_phase("opt 1/../x"), "opt-1-..-x")
        first = runner.unique_title("demo", "nominal", "baseline", 1)
        second = runner.unique_title("demo", "nominal", "baseline", 1)
        self.assertNotEqual(first, second)
        self.assertTrue(first.startswith("tokens:demo:nominal:baseline:1:"))
        with self.assertRaises(runner.RunnerError):
            runner.sanitize_phase("///")

    def test_load_scenarios_fills_defaults_and_rejects_empty(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            (directory / "nominal.json").write_text(
                json.dumps({"prompt": "do it"}), encoding="utf-8"
            )
            scenarios = runner.load_scenarios(directory)
            self.assertEqual(scenarios[0]["id"], "nominal")
            self.assertEqual(scenarios[0]["assertions"], {})
            with self.assertRaises(runner.RunnerError):
                runner.load_scenarios(directory / "absent")

    def test_load_scenarios_rejects_unsafe_ids(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            (directory / "bad.json").write_text(
                json.dumps({"id": "../bad", "prompt": "do it"}), encoding="utf-8"
            )
            with self.assertRaises(runner.RunnerError):
                runner.load_scenarios(directory)

    def test_copy_project_excludes_heavy_and_generated_directories(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source"
            destination = root / "destination"
            (source / ".git").mkdir(parents=True)
            (source / ".git" / "config").write_text("git", encoding="utf-8")
            (source / "target").mkdir()
            (source / "target" / "classes").write_text("class", encoding="utf-8")
            (source / "keep.txt").write_text("keep", encoding="utf-8")
            runner.copy_project(source, destination)
            self.assertTrue((destination / "keep.txt").is_file())
            self.assertFalse((destination / ".git").exists())
            self.assertFalse((destination / "target").exists())

    def test_ensure_history_creates_template_once(self):
        with tempfile.TemporaryDirectory() as tmp:
            history = Path(tmp) / "TOKENS_HISTORY.md"
            runner.ensure_history(history, "demo")
            first = history.read_text(encoding="utf-8")
            runner.ensure_history(history, "demo")
            self.assertEqual(history.read_text(encoding="utf-8"), first)
            self.assertIn("TOKENS_HISTOGRAM:START", first)

    def test_evaluate_assertions_covers_files_text_and_artifacts(self):
        with tempfile.TemporaryDirectory() as tmp:
            sandbox = Path(tmp)
            (sandbox / ".opencode" / "skills" / "demo").mkdir(parents=True)
            (sandbox / "out.txt").write_text("result: 42\n", encoding="utf-8")
            assertions = {
                "files": ["out.txt"],
                "contains": [{"path": "out.txt", "text": "42"}],
                "not_contains": [{"path": "out.txt", "text": "forbidden"}],
                "final_text_contains": ["done"],
                "commands": [[sys.executable, "-c", "print('ok')"]],
                "artifacts": ["out.txt"],
            }
            passed, details, artifacts = runner.evaluate_assertions(
                sandbox, "demo", assertions, "all done", 30, skip_rule_tests=True
            )
            self.assertTrue(passed, details)
            self.assertEqual(artifacts["out.txt"], runner.hash_file(sandbox / "out.txt"))

    def test_evaluate_assertions_fails_on_missing_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            passed, details, _ = runner.evaluate_assertions(
                Path(tmp),
                "demo",
                {"files": ["absent.txt"]},
                "",
                30,
                skip_rule_tests=True,
            )
            self.assertFalse(passed)
            self.assertEqual(details[0]["status"], "FAIL")


class IntegrationTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name)
        self.project = self.root / "project"
        self.skill = self.project / ".opencode" / "skills" / "demo"
        self.skill.mkdir(parents=True)
        (self.skill / "SKILL.md").write_text("# Demo\n", encoding="utf-8")
        self.scenarios = self.root / "scenarios"
        self.scenarios.mkdir()
        (self.scenarios / "nominal.json").write_text(
            json.dumps(
                {
                    "id": "nominal",
                    "prompt": "Do the demo task.",
                    "reason": "integration",
                    "assertions": {
                        "files": ["out.txt"],
                        "contains": [{"path": "out.txt", "text": "ok"}],
                        "rule_tests": False,
                    },
                }
            ),
            encoding="utf-8",
        )
        self.history = self.root / "TOKENS_HISTORY.md"
        self.db = self.root / "opencode.db"
        self._make_db()
        self.fake = self.root / "fake-opencode"
        self.fake.write_text(
            """#!/usr/bin/env python3
import json, os
from pathlib import Path
state = Path(os.environ["FAKE_OPENCODE_STATE"])
count = int(state.read_text()) + 1 if state.exists() else 1
state.write_text(str(count))
Path("out.txt").write_text("ok\\n")
print(json.dumps({"type": "step_start", "sessionID": f"ses_{os.environ['FAKE_PREFIX']}_{count}"}))
print(json.dumps({"type": "text", "part": {"text": "done"}}))
""",
            encoding="utf-8",
        )
        self.fake.chmod(0o755)

    def tearDown(self):
        self._tmp.cleanup()

    def _make_db(self):
        connection = sqlite3.connect(self.db)
        connection.execute(
            """
            CREATE TABLE session_v2 (
                id TEXT PRIMARY KEY, parent_id TEXT, title TEXT, directory TEXT,
                agent TEXT, model TEXT, cost REAL, tokens_input INTEGER,
                tokens_output INTEGER, tokens_reasoning INTEGER,
                tokens_cache_read INTEGER, tokens_cache_write INTEGER,
                time_created INTEGER, time_updated INTEGER
            )
            """
        )
        now = int(time.time() * 1000)
        for prefix, tokens in (
            ("base", (600, 150, 50, 200, 0)),
            ("cand", (500, 100, 50, 150, 0)),
        ):
            for index in range(1, 4):
                connection.execute(
                    "INSERT INTO session_v2 VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    (
                        f"ses_{prefix}_{index}",
                        None,
                        "fake",
                        str(self.root),
                        "build",
                        '{"id":"big-pickle","providerID":"opencode","variant":"default"}',
                        0.0,
                        *tokens,
                        now,
                        now,
                    ),
                )
        connection.commit()
        connection.close()

    def _run_phase(self, phase: str, prefix: str) -> int:
        old = dict(os.environ)
        os.environ.update(
            {
                "OPENCODE_DB": str(self.db),
                "FAKE_OPENCODE_STATE": str(self.root / "state"),
                "FAKE_PREFIX": prefix,
            }
        )
        try:
            with contextlib.redirect_stdout(io.StringIO()):
                return runner.main(
                    [
                        "--target",
                        "demo",
                        "--phase",
                        phase,
                        "--runs",
                        "1",
                        "--model",
                        "opencode/big-pickle#default",
                        "--agent",
                        "build",
                        "--project-root",
                        str(self.project),
                        "--scenarios-dir",
                        str(self.scenarios),
                        "--runs-dir",
                        str(self.root / "runs" / phase),
                        "--history",
                        str(self.history),
                        "--opencode-bin",
                        str(self.fake),
                    ]
                )
        finally:
            os.environ.clear()
            os.environ.update(old)

    def test_two_phases_measure_append_and_render(self):
        self.assertEqual(self._run_phase("baseline", "base"), 0)
        self.assertEqual(self._run_phase("opt-1", "cand"), 0)

        baseline = json.loads(
            next((self.root / "runs" / "baseline").rglob("run-1.json")).read_text(
                encoding="utf-8"
            )
        )
        candidate = json.loads(
            next((self.root / "runs" / "opt-1").rglob("run-1.json")).read_text(
                encoding="utf-8"
            )
        )
        self.assertEqual(baseline["result"], "PASS")
        self.assertEqual(candidate["result"], "PASS")
        self.assertEqual(baseline["tokens"]["total"], 1000)
        self.assertEqual(candidate["tokens"]["total"], 800)
        text = self.history.read_text(encoding="utf-8")
        self.assertIn("`ses_base_1`", text)
        self.assertIn("`ses_cand_2`", text)
        self.assertIn("Total sessions: 2", text)
        self.assertIn("1,000", text)
        self.assertIn("800", text)


if __name__ == "__main__":
    unittest.main()
