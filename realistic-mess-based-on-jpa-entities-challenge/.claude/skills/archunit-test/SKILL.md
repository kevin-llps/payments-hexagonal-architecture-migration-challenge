---
name: ArchUnit Test
description: Write and maintain ArchUnit tests that enforce the hexagonal architecture rules of a Java/Maven multi-module project at build time, in the composition-root module (bootstrap). Use when writing, adding, or tightening ArchUnit architecture tests in the composition-root module.
---

# ArchUnit Test

## Invocation Announcement

Before any other output or tool call, display exactly this line as the skill's first output:

`▶ Skill invoked: ArchUnit Test — write ArchUnit tests enforcing hexagonal architecture rules in the composition root`

Display it on every invocation, whether triggered by the user, an agent, or a skill. When delegating to a subagent or loading a skill, do not print its announcement on its behalf; it prints its own. The announcement never replaces or delays a mandatory approval gate.

## Purpose

Write and maintain ArchUnit tests that enforce the hexagonal architecture
rules of a Java/Maven multi-module project at build time, in the
composition-root module (`bootstrap`), the only module whose test classpath
sees every production class.

The skill is reusable: it holds **how** to write ArchUnit tests. **When** they
are created or extended (migration phases, iterations, tracking) stays the
responsibility of the calling agent (e.g.
`incremental-hexagonal-architecture-migration`).

## Scope

**In scope**

- Collecting the required inputs and reading the rule sources.
- The ArchUnit dependency setup of the target module.
- Writing and tightening ArchUnit rules, one test class per rule family, in
  the target module.
- Narrow, referenced temporary exceptions, reported to the caller.
- Running the ArchUnit tests and reporting the rule catalogue and results.

**Out of scope**

- Deciding the architecture, layers, packages, or ports: derive them from the rule sources; never invent them. If a source is missing or ambiguous, ask.
- Migrating or refactoring production code to satisfy a rule.
- Deciding when tests are created in a migration workflow or tracking them in migration documents (the calling agent's job).
- Disabling (`@Disabled`) or emptying an ArchUnit test, or weakening a rule to make it pass, without explicit user approval.
- Adding ArchUnit to any module other than the target module.

## Required inputs

- **Target module**: the composition-root module hosting the tests
  (default `bootstrap`).
- **Base package**: the root package to analyze (e.g.
  `fr.kevin.llps.payments`).
- **Rule sources**: the approved documents the rules derive from (e.g.
  `docs/migration-{date}/isolated-domain-design.md`,
  `docs/migration-{date}/technical-layer-design.md`,
  `docs/java-coding-conventions.md`).
- **Rules requested**: the rule families or individual rules to add or
  tighten.

Do not write any file while a required input is missing: no POM change, no
test class, no dependency. Ask for all missing inputs in a
single batched question, never one at a time. Do not ask again for inputs already given. The
target module may default to `bootstrap`.

## Workflow

1. Collect the required inputs; stop and ask in one batched question if any is missing.
2. Read the rule sources and the existing ArchUnit tests, if any, and resolve the real package names.
3. Check the dependency setup; add it if missing.
4. List the rules to add or tighten as a catalogue (rule, target, reason, source).
5. Write each rule in its rule-family class with its `.because(...)` reason.
6. Run the ArchUnit tests; report RED/GREEN per rule and any exception.
7. Return the catalogue, the files changed, and the test results.

## Dependency setup

- Declare `com.tngtech.archunit:archunit-junit5` only in the target module's POM, with `test` scope.

```xml
<!-- <target-module>/pom.xml -->
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

- Manage its version in the parent POM `dependencyManagement`; the module declaration carries no version.

```xml
<!-- parent pom.xml -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>${archunit.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- Never add ArchUnit to any other module, even in `test` scope.

## Test location

All ArchUnit test classes live in `<target-module>/src/test/java/<base-package-path>/architecture/`
(e.g. `bootstrap/src/test/java/fr/kevin/llps/payments/architecture/`).

## Test style

- Annotate each test class with `@AnalyzeClasses(packages = "<base-package>", importOptions = ImportOption.DoNotIncludeTests.class)` and declare every rule as a `static final ArchRule` field annotated with `@ArchTest`.
- The `ImportOption.DoNotIncludeTests` option excludes test classes from analysis, so only production classes are checked.
- One rule = one architectural constraint, each with an explicit `.because(...)` reason naming the source document.
- Keep ArchUnit's default `failOnEmptyShould`: a rule targeting a package that does not exist yet is added in the iteration that creates that package, and is never made to pass on an empty package (no `allowEmptyShould(true)`, no `archRule.failOnEmptyShould=false`).

```java
@AnalyzeClasses(packages = "fr.kevin.llps.payments", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainIsolationTest {

    @ArchTest
    static final ArchRule domainShouldNotDependOnSpring =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("isolated-domain-design.md: the domain has no framework dependencies");
}
```

## Rule families

Write one test class per rule family; never mix families in one class.

- `HexagonalArchitectureTest`: domain, ports, and adapters layers via `onionArchitecture()` (or `layeredArchitecture()`), consistent with the technical layer design.
- `DomainIsolationTest`: domain classes do not depend on Spring, Jakarta Persistence / Hibernate, Jackson, Kafka, JDBC, or any adapter or composition-root package.
- `AdapterDependenciesTest`: client adapters and infrastructure adapters do not depend on each other nor on the composition root; outbound adapters implement outbound ports; inbound adapters call inbound ports only.
- `PortsTest`: ports are interfaces located in the domain port packages defined by the isolated domain design.
- `CyclesTest`: no package cycles between slices (`slices().matching(...).should().beFreeOfCycles()`).
- `CodingConventionsTest`: the verifiable rules of the Java coding conventions: no field injection, no `System.out`, adapter naming prefixes, no public mutable fields in the domain.

## Running the tests

- Test-first: a new or tightened rule is written and run **before** the code it constrains is migrated; report its RED or GREEN result honestly.
- Run the ArchUnit tests from the target module (or the project's equivalent command):

```sh
mvn test -Dtest='*ArchitectureTest,*IsolationTest,*DependenciesTest,PortsTest,CyclesTest,CodingConventionsTest'
```

- They analyze bytecode only and must run without Docker, database, or broker.
- Report the results as actually observed, per rule; never claim GREEN without a run.

## Temporary exceptions

- An exception is allowed only as an explicit, narrow exception on the rule itself: `ignoreDependency(...)` for one named dependency, or `FreezingArchRule` for a recorded set of existing violations.
- Each exception carries a comment referencing the iteration or ticket ID that will remove it.
- Always report every exception to the caller (rule, scope of the exception, reference) so it can be tracked and removed.

```java
private static final ArchRule domainShouldNotDependOnJpa =
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                .because("isolated-domain-design.md: no JPA in the domain");
// Temporary: IT-03 removes this freeze once InstantPayment is migrated.
@ArchTest
static final ArchRule frozenDomainShouldNotDependOnJpa = FreezingArchRule.freeze(domainShouldNotDependOnJpa);
```

## Report

Return to the caller:

1. The rule catalogue, one row per rule: rule, target, reason, source document, result, exceptions.
2. The files changed (POMs and test classes).
3. The test results: the exact command and the observed RED/GREEN per rule.

## Rules

| ID  | Rule                                                                                       | Check            |
| --- | ------------------------------------------------------------------------------------------ | ---------------- |
| R1  | The skill is discoverable and its description says when to use it                          | static           |
| R2  | The first output line announces the skill invocation                                       | static           |
| R3  | The four required inputs are named, target module defaulting to bootstrap                  | static           |
| R4  | No file is written while an input is missing; missing inputs asked in one batched question | static           |
| R5  | ArchUnit is archunit-junit5 with test scope in the target module's POM                     | static           |
| R6  | The ArchUnit version is managed in the parent POM dependencyManagement                     | static           |
| R7  | ArchUnit is never added to any other module, even in test scope                            | static           |
| R8  | Tests live in `<target-module>/src/test/java/<base-package-path>/architecture/`            | static           |
| R9  | One test class per rule family, with the six named families                                | static           |
| R10 | Each rule family states its architectural constraints                                      | static           |
| R11 | Every rule is an @ArchTest field in an @AnalyzeClasses class                               | static           |
| R12 | Test classes are excluded from analysis with ImportOption.DoNotIncludeTests                | static           |
| R13 | One rule enforces one constraint and carries a .because(...) reason                        | static           |
| R14 | Packages, ports, and layers come from the rule sources; nothing is invented                | static           |
| R15 | failOnEmptyShould is kept; no rule is made to pass on an empty package                     | static           |
| R16 | Temporary exceptions are explicit, narrow, and reference an iteration or ticket            | static           |
| R17 | Every exception is reported to the caller                                                  | static           |
| R18 | No test is disabled or emptied and no rule weakened without explicit user approval         | static           |
| R19 | Rules are written test-first and RED or GREEN is reported honestly                         | static           |
| R20 | ArchUnit tests run without Docker and results are reported as actually observed            | static           |
| R21 | The report contains the rule catalogue, the files changed, and the results                 | static           |
| R22 | Architecture decisions, production migration, and test timing stay out of scope           | static           |
| R23 | Missing inputs are answered with one batched question and no file written                  | agent-evaluation |
| R24 | Disabling a failing test or relaxing it onto an empty package is declined                  | agent-evaluation |

## Acceptance criteria

- [ ] The first output line is `▶ Skill invoked: ArchUnit Test — write ArchUnit tests enforcing hexagonal architecture rules in the composition root`.
- [ ] No file is written while a required input is missing; missing inputs are requested in one batched question.
- [ ] ArchUnit is a `test`-scoped dependency of the target module only, with its version managed in the parent POM.
- [ ] Tests live in `<target-module>/src/test/java/<base-package-path>/architecture/`, one class per rule family.
- [ ] Every rule uses `@ArchTest`, excludes test classes from analysis, and has a `.because(...)` reason.
- [ ] Every package, port, and layer comes from the rule sources; nothing is invented.
- [ ] Rules are not made to pass on empty packages.
- [ ] Every exception is explicit, narrow, referenced, and reported; no test is disabled or emptied.
- [ ] Test results are reported as actually observed, and the tests run without Docker.
- [ ] The report contains the rule catalogue, the files changed, and the results.
