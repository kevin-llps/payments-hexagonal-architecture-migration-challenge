---
name: Test Driven Development
description: Drive the development of one public Java method test-first, one atomic rule at a time (RED, implement, GREEN). Use when a feature must be implemented with TDD from a feature description and its acceptance criteria.
---

# Test Driven Development

## Purpose

The development of a given feature is driven by tests: every behavior of the
method is first written as a failing test, then implemented until that test
passes, one atomic rule at a time.

## Required inputs

1. **Feature description** — what the method must do.
2. **Acceptance criteria** — the observable, checkable conditions the method
   must satisfy.

Both are required to execute this skill. If either is missing, ask the user for
every missing item in a single question and stop. Do not define a signature,
write a test, or write any implementation until both are present.

## Scope

**In scope**

- Test developments by successive iterations compliant with the TDD approach.
- The scope of the development is a single public method.

**Out of scope**

- Reworking tests covering other features: existing tests of other features
  are never modified, deleted, or rewritten, even when they look related. If
  one of them fails, report it instead of editing it.

## Workflow

1. **Intake** — collect the feature description and the acceptance criteria.
   Stop and ask if either is missing. Identify exactly one public method to
   develop and the class that owns it; a second method is a second run.
2. **Decompose** — split the acceptance criteria into atomic rules:
   one rule, one observable behavior, one expected result. Number them
   `R1`, `R2`, ... in the order they will be implemented, and show the list
   before coding. A criterion that mixes several behaviors becomes several rules.
3. **Signature** — define the Java method with its final public signature and
   no implementation, before any test is written. The body returns `null` by
   default:

   ```java
   public BigDecimal computeFee(String chargeBearer, BigDecimal amount) {
       return null;
   }
   ```

   The code must compile at this point (for example `mvn test-compile`). Only
   a primitive return type, where `null` cannot compile, uses the type's
   default value (`0`, `false`) instead.
4. **Per-rule loop** — take the first atomic rule not yet done:
   1. **Test first** — write one test for the current rule only, in the test
      class of the method's class (JUnit 5, AssertJ assertions), named after
      the rule. The test states the expected result for that rule; do not
      write tests for later rules yet and do not touch the implementation.
   2. **Run the targeted test** with the project's build tool, selecting only
      that test, e.g. with Maven:
      `mvn test -Dtest=<TestClass>#<testMethod>`
   3. **RED** — the test must be RED: it fails on its assertion because the
      actual result does not match the expected result. Keep the failure
      message as the rule's RED evidence. A compilation error, an unrelated
      exception, or a test that already passes is not a valid RED: correct the
      test and rerun until it fails because the actual result differs from
      the expected one.
   4. **Implement the rule** — write the minimal code in the method that makes
      the current rule pass, and nothing for rules not yet tested. Change only
      the method under development (and private helpers it needs).
   5. **GREEN** — rerun the same targeted test. It must be GREEN: the
      actual result matches the expected result. Then run the whole test class
      (`mvn test -Dtest=<TestClass>`) so the rules already done stay GREEN. Keep the passing output as the rule's GREEN evidence. If the test
      is still RED, keep implementing; never weaken the assertion.
5. **Iterate on all atomic rules** — repeat step 4 for `R2`, `R3`, ... in
   order. Do not start the next rule before the current one is GREEN.
6. **Show all test rule results** — after the last rule, run the method's
   test class once more and report one row per atomic rule:

   | Rule | Test method | RED evidence | GREEN evidence |
   | ---- | ----------- | ------------ | -------------- |
   | R1   | `<testMethod>` | expected `<x>` but was `null` | PASS |

   followed by the final test-run summary. Never report a rule as done
   without both its RED and its GREEN evidence.

## Rules

| ID  | Rule                                                                                  | Check            |
| --- | ------------------------------------------------------------------------------------- | ---------------- |
| R1  | The skill is discoverable and its description states when to use it                  | static           |
| R2  | A feature description and acceptance criteria are both required before anything is written | static     |
| R3  | The development scope is a single public method                                       | static           |
| R4  | Tests covering other features are never reworked                                      | static           |
| R5  | Acceptance criteria are decomposed into atomic rules with stable IDs                  | static           |
| R6  | The Java method signature compiles without implementation, returning null by default  | static           |
| R7  | Exactly one atomic rule is tested first, before its implementation                    | static           |
| R8  | The targeted test is run with the project's build tool                                | static           |
| R9  | The test must be RED: the actual result does not match the expected result            | static           |
| R10 | Only the code needed by the current atomic rule is implemented                        | static           |
| R11 | The test must be GREEN: the actual result matches the expected result                 | static           |
| R12 | Every atomic rule is iterated in order; the next starts only once the current is GREEN | static          |
| R13 | All test rule results are shown with RED then GREEN evidence per rule                 | static           |
| R14 | The Rules table covers the spec bidirectionally and a README documents the skill      | static           |
| R15 | A request without acceptance criteria is answered by asking, never by writing code    | agent-evaluation |
| R16 | A complete request starts with rules and a null signature, then one rule RED then GREEN | agent-evaluation |

## Acceptance criteria

- [ ] Nothing (signature, test, implementation) is written while the feature
      description or the acceptance criteria are missing.
- [ ] The acceptance criteria are listed as atomic rules `R1`, `R2`, ...
      before any code is written.
- [ ] The method signature compiles and returns `null` before the first test.
- [ ] Every atomic rule has RED evidence (actual differs from expected) before
      its implementation and GREEN evidence after it.
- [ ] Only the single public method under development and its new tests are
      changed; tests covering other features are untouched.
- [ ] The final report shows the results of all test rules.
