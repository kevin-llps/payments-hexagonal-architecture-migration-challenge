#!/usr/bin/env python3
"""Verify a skill against its authoritative rule spec.

Usage:
    python3 run-skill-rule-tests.py <skill-dir|skill-id>

Reads <skill-dir>/tests/rules.json, validates the spec, runs the static
assertions against <skill-dir>/SKILL.md, then lists every agent-evaluation rule
with the prompt an agent must execute. Static rules come first; agent
evaluations are always listed after them and are never hidden behind a flag.

A bare skill ID is resolved from the current directory, then from the skills
root. With no SKILL.md the run is RED: every static rule fails and the exit code
is 1. A missing or malformed agent-evaluation fixture is also a failure.

Exit codes: 0 = every static rule passes and every agent-evaluation fixture is
usable, 1 = at least one static or structural failure, 2 = usage error or
malformed spec.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

KNOWN_ASSERTIONS = {
    "frontmatterField",
    "heading",
    "contains",
    "notContains",
    "regex",
    "pathExists",
    "idMatchesDir",
    "ruleTableCoverage",
    "scriptTestCoverage",
}

KNOWN_CHECKS = ("static", "agent-evaluation")

ID_PATTERN = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
RULE_ID_PATTERN = re.compile(r"^R\d+$")
FRONTMATTER_PATTERN = re.compile(r"^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)")
FRONTMATTER_STRIP_PATTERN = re.compile(r"^---\r?\n[\s\S]*?\r?\n---(?:\r?\n|$)")
FENCE_PATTERN = re.compile(r"^\s*(`{3,}|~{3,})")
HEADING_PATTERN = re.compile(r"^(#{1,6})\s+(.+?)\s*$")
RULE_ROW_PATTERN = re.compile(r"^\|\s*(R\d+)\s*\|")

# scripts/ -> skill-testing/ -> skills/
SKILLS_ROOT = Path(__file__).resolve().parents[2]


def fail(message: str) -> None:
    print(f"spec error: {message}", file=sys.stderr)
    raise SystemExit(2)


def usage() -> None:
    print(
        "usage: python3 run-skill-rule-tests.py <skill-dir|skill-id>",
        file=sys.stderr,
    )
    raise SystemExit(2)


def resolve_skill_dir(raw: str) -> Path:
    """Accept a path or a bare skill ID, resolved from cwd then the skills root."""
    candidate = Path(raw).expanduser()
    if candidate.is_absolute():
        return candidate.resolve()
    for base in (Path.cwd(), SKILLS_ROOT):
        resolved = (base / candidate).resolve()
        if (resolved / "tests" / "rules.json").is_file() or (resolved / "SKILL.md").is_file():
            return resolved
    return (Path.cwd() / candidate).resolve()


def parse_args(argv: list[str]) -> Path:
    if len(argv) != 1 or argv[0].startswith("-"):
        usage()
    return resolve_skill_dir(argv[0])


def parse_frontmatter(text: str) -> tuple[bool, dict[str, str]]:
    match = FRONTMATTER_PATTERN.match(text)
    if not match:
        return False, {}
    data: dict[str, str] = {}
    for line in match.group(1).splitlines():
        if not line.strip() or re.match(r"^\s", line):
            continue
        index = line.find(":")
        if index == -1:
            continue
        key = line[:index].strip()
        value = line[index + 1 :].strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {'"', "'"}:
            value = value[1:-1]
        data[key] = value
    return True, data


def strip_frontmatter(text: str) -> str:
    return FRONTMATTER_STRIP_PATTERN.sub("", text, count=1)


def scan_headings(body: str) -> list[dict[str, int | str]]:
    """Collect headings, ignoring those inside fenced code blocks."""
    headings: list[dict[str, int | str]] = []
    fence: str | None = None
    for index, line in enumerate(body.splitlines()):
        fence_match = FENCE_PATTERN.match(line)
        if fence_match:
            marker = fence_match.group(1)[0]
            if fence is None:
                fence = marker
            elif fence == marker:
                fence = None
            continue
        if fence is not None:
            continue
        heading = HEADING_PATTERN.match(line)
        if heading:
            headings.append(
                {
                    "index": index,
                    "level": len(heading.group(1)),
                    "text": re.sub(r"[*_`]", "", heading.group(2)).strip(),
                }
            )
    return headings


def get_section(body: str, name: str) -> str | None:
    lines = body.splitlines()
    headings = scan_headings(body)
    target = name.strip().lower()
    found = next((heading for heading in headings if str(heading["text"]).lower() == target), None)
    if found is None:
        return None
    next_heading = next(
        (
            heading
            for heading in headings
            if int(heading["index"]) > int(found["index"])
            and int(heading["level"]) <= int(found["level"])
        ),
        None,
    )
    end = int(next_heading["index"]) if next_heading else len(lines)
    return "\n".join(lines[int(found["index"]) + 1 : end])


def rule_table_ids(body: str) -> list[str] | None:
    section = get_section(body, "Rules")
    if section is None:
        return None
    return [match.group(1) for line in section.splitlines() if (match := RULE_ROW_PATTERN.match(line))]


def unique(values: list[str]) -> list[str]:
    seen: list[str] = []
    for value in values:
        if value not in seen:
            seen.append(value)
    return seen


def compare_ids(expected: list[str], actual: list[str]) -> tuple[list[str], list[str]]:
    missing = [value for value in expected if value not in actual]
    extra = [value for value in actual if value not in expected]
    return missing, extra


def script_test_coverage(directory: Path) -> list[str]:
    """Every script directly under scripts/ needs scripts/tests/test_<name>.py."""
    scripts_dir = directory / "scripts"
    if not scripts_dir.is_dir():
        return []
    tests_dir = scripts_dir / "tests"
    missing = []
    for script in sorted(scripts_dir.glob("*.py")):
        expected = tests_dir / f"test_{script.stem.replace('-', '_')}.py"
        if not expected.is_file():
            missing.append(f"{script.name} has no {expected.relative_to(directory)}")
    return missing


def python_flags(raw: str | None) -> int:
    flags = 0
    for char in raw or "":
        flags |= {"i": re.IGNORECASE, "m": re.MULTILINE, "s": re.DOTALL}.get(char, 0)
    return flags


def haystack_for(assertion: dict, body: str) -> tuple[str | None, str | None]:
    section_name = assertion.get("section")
    if not section_name:
        return body, None
    section = get_section(body, section_name)
    if section is None:
        return None, f'section "{section_name}" not found'
    return section, None


def evaluate(assertion: dict, ctx: dict) -> str | bool:
    body = ctx["body"]
    frontmatter = ctx["frontmatter"]
    directory = ctx["dir"]
    spec_ids = ctx["spec_ids"]
    value = assertion.get("value")

    kind = assertion.get("type")
    if kind == "frontmatterField":
        if not frontmatter["present"]:
            return "no frontmatter block"
        field = frontmatter["data"].get(value or "")
        if not field:
            return f"frontmatter.{value} is missing"
        if assertion.get("matches"):
            flags = python_flags(assertion.get("flags", "i"))
            if not re.search(assertion["matches"], field, flags):
                return f"frontmatter.{value} does not match /{assertion['matches']}/"
        return True
    if kind == "heading":
        if get_section(body, value or "") is None:
            return f'heading "{value}" not found'
        return True
    if kind in ("contains", "notContains"):
        haystack, error = haystack_for(assertion, body)
        if error:
            return error
        present = value in haystack
        if kind == "contains" and not present:
            return f'text not found: "{value}"'
        if kind == "notContains" and present:
            return f'forbidden text found: "{value}"'
        return True
    if kind == "regex":
        haystack, error = haystack_for(assertion, body)
        if error:
            return error
        flags = python_flags(assertion.get("flags"))
        if not re.search(value, haystack, flags):
            return f"pattern not found: /{value}/"
        return True
    if kind == "pathExists":
        if not (directory / value).exists():
            return f"path missing: {value}"
        return True
    if kind == "idMatchesDir":
        name = directory.name
        if not ID_PATTERN.match(name):
            return f'directory name "{name}" is not kebab-case'
        return True
    if kind == "ruleTableCoverage":
        actual = rule_table_ids(body)
        if actual is None:
            return 'heading "Rules" not found'
        missing, extra = compare_ids(spec_ids, unique(actual))
        problems = []
        if missing:
            problems.append(f"missing from table: {', '.join(missing)}")
        if extra:
            problems.append(f"not in spec: {', '.join(extra)}")
        return "; ".join(problems) if problems else True
    if kind == "scriptTestCoverage":
        problems = script_test_coverage(directory)
        return "; ".join(problems) if problems else True
    return f"unknown assertion type: {kind}"


def validate_spec(spec: object, directory: Path) -> list[str]:
    if not isinstance(spec, dict):
        fail("rules.json must be an object")
    skill = spec.get("skill")
    if not isinstance(skill, str) or not skill:
        fail("missing string field: skill")
    if skill != directory.name:
        fail(f'spec.skill "{skill}" does not match directory "{directory.name}"')
    rules = spec.get("rules")
    if not isinstance(rules, list) or not rules:
        fail("rules must be a non-empty array")
    ids: list[str] = []
    for rule in rules:
        rule_id = rule.get("id", "")
        if not RULE_ID_PATTERN.match(rule_id):
            fail(f"invalid rule id: {rule_id}")
        if rule_id in ids:
            fail(f"duplicate rule id: {rule_id}")
        ids.append(rule_id)
        if not isinstance(rule.get("rule"), str) or not rule["rule"].strip():
            fail(f"{rule_id} is missing its rule text")
        check = rule.get("check")
        if check not in KNOWN_CHECKS:
            fail(f'{rule_id} has invalid check: {check} (use "static" or "agent-evaluation")')
        if check == "static":
            assertions = rule.get("assertions")
            if not isinstance(assertions, list) or not assertions:
                fail(f"{rule_id} is static but has no assertions")
            for assertion in assertions:
                kind = assertion.get("type")
                if kind not in KNOWN_ASSERTIONS:
                    fail(f"{rule_id} has unknown assertion type: {kind}")
                if kind not in ("idMatchesDir", "ruleTableCoverage", "scriptTestCoverage"):
                    if not isinstance(assertion.get("value"), str) or assertion["value"] == "":
                        fail(f"{rule_id} {kind} assertion has no value")
    return ids


def read_spec(directory: Path) -> dict:
    spec_path = directory / "tests" / "rules.json"
    if not spec_path.exists():
        print(f"spec error: missing {spec_path}", file=sys.stderr)
        print(
            "hint: pass the skill directory or a bare skill ID, "
            "e.g. .opencode/skills/<id> or <id>",
            file=sys.stderr,
        )
        raise SystemExit(1)
    try:
        return json.loads(spec_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        fail(f"cannot parse tests/rules.json: {error}")


def read_agent_evaluation(directory: Path, rule_id: str) -> tuple[str, str] | str:
    """Return (prompt, expected) or a failure reason."""
    fixture = directory / "tests" / "agent-evaluations" / f"{rule_id}.md"
    if not fixture.exists():
        return f"fixture missing: tests/agent-evaluations/{rule_id}.md"
    body = strip_frontmatter(fixture.read_text(encoding="utf-8"))
    prompt = get_section(body, "Prompt")
    expected = get_section(body, "Expected")
    if prompt is None or not prompt.strip() or expected is None or not expected.strip():
        return 'fixture needs non-empty "Prompt" and "Expected" sections'
    return prompt.strip(), expected.strip()


def print_indented(label: str, text: str) -> None:
    print(f"      {label}:")
    for line in text.splitlines():
        print(f"        {line}" if line else "")


def main() -> None:
    directory = parse_args(sys.argv[1:])
    spec = read_spec(directory)
    spec_ids = validate_spec(spec, directory)

    print(f"Skill: {directory.name}")
    print(f"Spec:  {directory / 'tests' / 'rules.json'}")
    print(f"Rules: {len(spec['rules'])}")
    print("")

    skill_path = directory / "SKILL.md"
    skill_exists = skill_path.exists()
    full_text = skill_path.read_text(encoding="utf-8") if skill_exists else ""
    frontmatter_present, frontmatter_data = parse_frontmatter(full_text)
    body = strip_frontmatter(full_text)
    ctx = {
        "body": body,
        "full_text": full_text,
        "frontmatter": {"present": frontmatter_present, "data": frontmatter_data},
        "dir": directory,
        "spec_ids": spec_ids,
    }

    passed = 0
    failed = 0
    agent_ready: list[tuple[dict, str, str]] = []
    agent_unusable: list[tuple[dict, str]] = []

    for rule in spec["rules"]:
        if rule["check"] == "agent-evaluation":
            fixture = read_agent_evaluation(directory, rule["id"])
            if isinstance(fixture, str):
                agent_unusable.append((rule, fixture))
            else:
                agent_ready.append((rule, fixture[0], fixture[1]))
            continue

        if not skill_exists:
            failed += 1
            print(f"FAIL  {rule['id']}  {rule['rule']}")
            print("        - SKILL.md not found")
            continue

        problems = []
        for assertion in rule["assertions"]:
            result = evaluate(assertion, ctx)
            if result is not True:
                problems.append(result)

        if problems:
            failed += 1
            print(f"FAIL  {rule['id']}  {rule['rule']}")
            for problem in problems:
                print(f"        - {problem}")
        else:
            passed += 1
            print(f"PASS  {rule['id']}  {rule['rule']}")

    print("")
    print(f"Static: {passed} passed, {failed} failed, {passed + failed} total")

    if agent_ready or agent_unusable:
        print("")
        print("Agent evaluations (not executed by this script — run with an agent):")
        for rule, prompt, expected in agent_ready:
            print(f"  AGENT-EVAL  {rule['id']}  {rule['rule']}")
            print_indented("Prompt", prompt)
            print_indented("Expected", expected)
        for rule, reason in agent_unusable:
            print(f"  FAIL  {rule['id']}  {rule['rule']}")
            print(f"        - {reason}")
        print("")
        summary = f"Agent evaluations to run: {len(agent_ready)}"
        if agent_unusable:
            summary += f", {len(agent_unusable)} unusable"
        print(summary)

    print("")
    if failed or agent_unusable:
        print("Result: RED")
    elif agent_ready:
        print(f"Result: GREEN (static) — {len(agent_ready)} agent evaluation(s) pending")
    else:
        print("Result: GREEN")

    raise SystemExit(1 if failed or agent_unusable else 0)


if __name__ == "__main__":
    main()
