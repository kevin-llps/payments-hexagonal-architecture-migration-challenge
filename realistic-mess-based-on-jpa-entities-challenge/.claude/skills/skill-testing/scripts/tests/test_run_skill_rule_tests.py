"""Unit and CLI tests for run-skill-rule-tests.py.

Run with:
    python3 -m unittest discover -s .opencode/skills/skill-testing/scripts/tests -v
"""

from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
SCRIPT = SCRIPTS / "run-skill-rule-tests.py"


def load_module(name: str):
    spec = importlib.util.spec_from_file_location(name.replace("-", "_"), SCRIPTS / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


runner = load_module("run-skill-rule-tests")


def run_cli(*args: str, cwd: Path | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args],
        capture_output=True,
        text=True,
        cwd=cwd,
    )


STATIC_RULE = {
    "id": "R1",
    "rule": "Has a name",
    "check": "static",
    "assertions": [{"type": "frontmatterField", "value": "name"}],
}

SKILL_MD = """---
name: Demo
description: Demo skill. Use when testing.
---

# Demo
"""


class HelperTests(unittest.TestCase):
    def test_parse_frontmatter_reads_top_level_scalars(self):
        present, data = runner.parse_frontmatter(SKILL_MD)
        self.assertTrue(present)
        self.assertEqual(data["name"], "Demo")
        self.assertIn("Use when", data["description"])

    def test_parse_frontmatter_reports_absent_block(self):
        present, data = runner.parse_frontmatter("# No frontmatter\n")
        self.assertFalse(present)
        self.assertEqual(data, {})

    def test_headings_inside_fences_are_not_sections(self):
        body = (
            "## Rules\n\n| R1 | a | static |\n\n"
            "```markdown\n## Rules\nfake table\n```\n\nstill in Rules\n\n"
            "## Scope\n\nreal scope\n"
        )
        section = runner.get_section(body, "Rules")
        self.assertIn("still in Rules", section)
        self.assertEqual(runner.get_section(body, "Scope").strip(), "real scope")

    def test_get_section_stops_at_same_level_heading(self):
        body = "## A\nfirst\n## B\nsecond\n"
        self.assertEqual(runner.get_section(body, "A").strip(), "first")

    def test_get_section_returns_none_when_missing(self):
        self.assertIsNone(runner.get_section("## A\n", "B"))

    def test_rule_table_ids_reads_first_column(self):
        body = "## Rules\n\n| ID | Rule | Check |\n| -- | ---- | ----- |\n| R1 | a | static |\n| R12 | b | eval |\n"
        self.assertEqual(runner.rule_table_ids(body), ["R1", "R12"])

    def test_python_flags_maps_letters(self):
        self.assertEqual(runner.python_flags("i"), runner.re.IGNORECASE)
        self.assertEqual(runner.python_flags("im"), runner.re.IGNORECASE | runner.re.MULTILINE)
        self.assertEqual(runner.python_flags(None), 0)


class CliTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.tmp = Path(self._tmp.name)
        self.skill = self.tmp / "demo"
        (self.skill / "tests").mkdir(parents=True)
        self.write_rules([STATIC_RULE])
        self.write_skill(SKILL_MD)

    def tearDown(self):
        self._tmp.cleanup()

    def write_rules(self, rules, skill_id="demo"):
        (self.skill / "tests" / "rules.json").write_text(
            json.dumps({"skill": skill_id, "rules": rules}), encoding="utf-8"
        )

    def write_skill(self, text):
        (self.skill / "SKILL.md").write_text(text, encoding="utf-8")

    def write_fixture(self, rule_id, text):
        directory = self.skill / "tests" / "agent-evaluations"
        directory.mkdir(parents=True, exist_ok=True)
        (directory / f"{rule_id}.md").write_text(text, encoding="utf-8")

    def test_static_pass_is_green(self):
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("PASS  R1", result.stdout)
        self.assertIn("Result: GREEN", result.stdout)

    def test_selected_rule_runs_only_that_rule(self):
        rules = [
            STATIC_RULE,
            {
                "id": "R2",
                "rule": "Has a workflow",
                "check": "static",
                "assertions": [{"type": "heading", "value": "Workflow"}],
            },
        ]
        self.write_rules(rules)
        result = run_cli(str(self.skill), "--rule", "R1")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("PASS  R1", result.stdout)
        self.assertNotIn("R2", result.stdout)
        self.assertIn("Selected: R1", result.stdout)
        self.assertIn("Result: GREEN", result.stdout)

    def test_selected_failing_rule_is_red(self):
        rules = [
            STATIC_RULE,
            {
                "id": "R2",
                "rule": "Has a workflow",
                "check": "static",
                "assertions": [{"type": "heading", "value": "Workflow"}],
            },
        ]
        self.write_rules(rules)
        result = run_cli(str(self.skill), "--rule", "R2")
        self.assertEqual(result.returncode, 1)
        self.assertIn("FAIL  R2", result.stdout)
        self.assertIn('heading "Workflow" not found', result.stdout)
        self.assertNotIn("R1", result.stdout)
        self.assertIn("Result: RED", result.stdout)

    def test_selected_rule_without_skill_md_is_red(self):
        (self.skill / "SKILL.md").unlink()
        result = run_cli(str(self.skill), "--rule", "R1")
        self.assertEqual(result.returncode, 1)
        self.assertIn("FAIL  R1", result.stdout)
        self.assertIn("SKILL.md not found", result.stdout)
        self.assertIn("Result: RED", result.stdout)

    def test_selected_unknown_rule_exits_two(self):
        result = run_cli(str(self.skill), "--rule", "R9")
        self.assertEqual(result.returncode, 2)
        self.assertIn("unknown rule id", result.stderr)
        self.assertNotIn("PASS", result.stdout)

    def test_selected_rule_accepts_equals_and_repeated_forms(self):
        rules = [
            STATIC_RULE,
            {
                "id": "R2",
                "rule": "Has a workflow",
                "check": "static",
                "assertions": [{"type": "heading", "value": "Workflow"}],
            },
        ]
        self.write_rules(rules)
        self.write_skill(SKILL_MD + "\n## Workflow\n\nstep\n")
        result = run_cli(str(self.skill), "--rule=R1", "--rule", "R2", "--rule", "R1")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(result.stdout.count("PASS  R1"), 1)
        self.assertIn("PASS  R2", result.stdout)
        self.assertIn("Selected: R1, R2 (2 of 2)", result.stdout)

    def test_empty_rule_value_is_rejected(self):
        result = run_cli(str(self.skill), "--rule=")
        self.assertEqual(result.returncode, 2)
        self.assertIn("usage:", result.stderr)
        self.assertNotIn("PASS", result.stdout)

    def test_selected_agent_evaluation_is_listed_alone(self):
        rules = [STATIC_RULE, {"id": "R2", "rule": "Asks first", "check": "agent-evaluation"}]
        self.write_rules(rules)
        self.write_fixture("R2", "# R2\n\n## Prompt\n\nDo the thing.\n\n## Expected\n\nIt asks first.\n")
        result = run_cli(str(self.skill), "--rule", "R2")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("AGENT-EVAL  R2", result.stdout)
        self.assertNotIn("PASS  R1", result.stdout)
        self.assertIn("1 agent evaluation(s) pending", result.stdout)

    def test_selected_agent_evaluation_missing_fixture_fails(self):
        rules = [STATIC_RULE, {"id": "R2", "rule": "Asks first", "check": "agent-evaluation"}]
        self.write_rules(rules)
        result = run_cli(str(self.skill), "--rule", "R2")
        self.assertEqual(result.returncode, 1)
        self.assertIn("fixture missing: tests/agent-evaluations/R2.md", result.stdout)
        self.assertIn("Result: RED", result.stdout)

    def test_selected_rule_still_validates_whole_spec(self):
        rules = [
            STATIC_RULE,
            {"id": "R2", "rule": "broken", "check": "nope"},
        ]
        self.write_rules(rules)
        result = run_cli(str(self.skill), "--rule", "R1")
        self.assertEqual(result.returncode, 2)
        self.assertIn("invalid check: nope", result.stderr)

    def test_missing_skill_md_is_red(self):
        (self.skill / "SKILL.md").unlink()
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 1)
        self.assertIn("SKILL.md not found", result.stdout)
        self.assertIn("Result: RED", result.stdout)

    def test_failing_assertion_reports_reason(self):
        self.write_skill("---\ndescription: no name. Use when testing.\n---\n")
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 1)
        self.assertIn("FAIL  R1", result.stdout)
        self.assertIn("frontmatter.name is missing", result.stdout)

    def test_agent_evaluation_is_listed_after_static_results(self):
        rules = [STATIC_RULE, {"id": "R2", "rule": "Asks first", "check": "agent-evaluation"}]
        self.write_rules(rules)
        self.write_fixture("R2", "# R2\n\n## Prompt\n\nDo the thing.\n\n## Expected\n\nIt asks first.\n")
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("AGENT-EVAL  R2", result.stdout)
        self.assertIn("Prompt:", result.stdout)
        self.assertIn("Expected:", result.stdout)
        self.assertLess(result.stdout.index("Static:"), result.stdout.index("AGENT-EVAL"))
        self.assertIn("1 agent evaluation(s) pending", result.stdout)

    def test_missing_agent_evaluation_fixture_fails(self):
        self.write_rules([STATIC_RULE, {"id": "R2", "rule": "Asks first", "check": "agent-evaluation"}])
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 1)
        self.assertIn("fixture missing: tests/agent-evaluations/R2.md", result.stdout)
        self.assertIn("Result: RED", result.stdout)

    def test_malformed_agent_evaluation_fixture_fails(self):
        self.write_rules([STATIC_RULE, {"id": "R2", "rule": "Asks first", "check": "agent-evaluation"}])
        self.write_fixture("R2", "# R2\n\n## Prompt\n\nDo the thing.\n")
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 1)
        self.assertIn('needs non-empty "Prompt" and "Expected"', result.stdout)

    def test_old_eval_check_name_is_rejected(self):
        self.write_rules([{"id": "R1", "rule": "x", "check": "eval"}])
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 2)
        self.assertIn('invalid check: eval (use "static" or "agent-evaluation")', result.stderr)

    def test_eval_flag_is_rejected(self):
        result = run_cli(str(self.skill), "--eval")
        self.assertEqual(result.returncode, 2)
        self.assertIn("usage:", result.stderr)

    def test_unknown_assertion_type_is_rejected(self):
        self.write_rules([{"id": "R1", "rule": "x", "check": "static", "assertions": [{"type": "nope", "value": "x"}]}])
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 2)
        self.assertIn("unknown assertion type: nope", result.stderr)

    def test_spec_skill_must_match_directory(self):
        self.write_rules([STATIC_RULE], skill_id="other")
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 2)
        self.assertIn("does not match directory", result.stderr)

    def test_rule_table_coverage_mismatch_fails(self):
        rule = {
            "id": "R1",
            "rule": "Rules table matches spec",
            "check": "static",
            "assertions": [{"type": "ruleTableCoverage"}],
        }
        self.write_rules([rule])
        self.write_skill(SKILL_MD + "\n## Rules\n\n| ID | Rule | Check |\n| -- | ---- | ----- |\n| R1 | a | static |\n")
        self.assertEqual(run_cli(str(self.skill)).returncode, 0)

        self.write_skill(SKILL_MD + "\n## Rules\n\n| ID | Rule | Check |\n| -- | ---- | ----- |\n| R1 | a | static |\n| R9 | extra | static |\n")
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 1)
        self.assertIn("not in spec: R9", result.stdout)

    def test_section_scoped_contains(self):
        rule = {
            "id": "R1",
            "rule": "Scope stays out of workflow",
            "check": "static",
            "assertions": [{"type": "contains", "value": "forbidden", "section": "Workflow"}],
        }
        self.write_rules([rule])
        self.write_skill(SKILL_MD + "\n## Workflow\n\nallowed\n\n## Scope\n\nforbidden\n")
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 1)
        self.assertIn('text not found: "forbidden"', result.stdout)

    def test_bare_skill_id_resolves_from_cwd(self):
        result = run_cli("demo", cwd=self.tmp)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("Skill: demo", result.stdout)

    def test_missing_spec_exits_one_with_hint(self):
        (self.skill / "tests" / "rules.json").unlink()
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 1)
        self.assertIn("hint: pass the skill directory or a bare skill ID", result.stderr)

    def test_script_test_coverage_passes_when_test_module_exists(self):
        rule = {"id": "R1", "rule": "Every script is tested", "check": "static", "assertions": [{"type": "scriptTestCoverage"}]}
        self.write_rules([rule])
        (self.skill / "scripts" / "tests").mkdir(parents=True)
        (self.skill / "scripts" / "do-thing.py").write_text("", encoding="utf-8")
        (self.skill / "scripts" / "tests" / "test_do_thing.py").write_text("", encoding="utf-8")
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("PASS  R1", result.stdout)

    def test_script_test_coverage_fails_without_test_module(self):
        rule = {"id": "R1", "rule": "Every script is tested", "check": "static", "assertions": [{"type": "scriptTestCoverage"}]}
        self.write_rules([rule])
        (self.skill / "scripts").mkdir()
        (self.skill / "scripts" / "do-thing.py").write_text("", encoding="utf-8")
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 1)
        self.assertIn("do-thing.py has no scripts/tests/test_do_thing.py", result.stdout)

    def test_script_test_coverage_ignores_skill_without_scripts(self):
        rule = {"id": "R1", "rule": "Every script is tested", "check": "static", "assertions": [{"type": "scriptTestCoverage"}]}
        self.write_rules([rule])
        result = run_cli(str(self.skill))
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
