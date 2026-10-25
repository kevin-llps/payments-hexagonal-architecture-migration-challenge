---
name: Single Responsibility Principle
description: Apply the Single Responsibility Principle before creating classes so each proposed class has one responsibility and one primary reason to change. Use when designing or creating new classes for a feature, before writing any implementation.
---

# Single Responsibility Principle

Design classes so that each one has a single, cohesive responsibility, and do
it before any class exists. Analysis and design come first; implementation
comes last.

## Purpose

Apply the Single Responsibility Principle before creating any classes to ensure that each proposed class has one clearly defined responsibility and one primary reason to change.

## Scope

**In scope**

- Analyze the feature requirements before designing or creating classes.
- Identify the distinct business responsibilities involved in the feature.
- Define class boundaries and responsibilities before implementation.
- Ensure the proposed design follows SRP from the beginning rather than relying on subsequent refactoring.

**Out of scope**

- Other design principles (Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion); each has its own skill.
- Refactoring existing classes after the fact as the primary goal; this skill shapes the design before implementation.
- Adding, inventing, or changing business requirements.
- Architectural changes unrelated to the classes needed by the feature.

## Design guidelines

### Analyze requirements first

Before creating classes:

- Read and understand the feature description and acceptance criteria.
- Identify the business rules, operations, and external concerns involved.
- Group related behaviors according to their responsibility and reasons for changing.
- Distinguish core business logic from supporting concerns such as persistence, validation, formatting, and external communication when they represent separate responsibilities.

### Define class responsibilities

For each proposed class:

- Define its purpose in one clear sentence.
- Identify the behavior and data that belong to it.
- Establish its boundaries and responsibilities.
- Verify that its methods serve the same cohesive purpose.
- Ensure that unrelated business concerns do not share the same class merely because they belong to the same feature.

### Determine class boundaries

- Create separate classes when responsibilities have independent reasons to change.
- Keep closely related data and behavior together.
- Assign each responsibility to the class best suited to own it.
- Define how classes collaborate before implementing their behavior.
- Use dependency injection when appropriate to connect collaborating classes.
- Follow the project's existing architectural and package conventions.

### Avoid overengineering

- Do not create classes solely to reduce the number of methods in another class.
- Do not introduce unnecessary interfaces, abstractions, utilities, or delegation layers.
- Do not create one class per method.
- Keep the design as simple as possible while maintaining clear responsibility boundaries.
- Prefer a cohesive class over an artificial separation that adds complexity without a meaningful benefit.

## Workflow

Follow these steps before implementation, in order:

1. **Analyze:** Understand the feature requirements and identify its distinct responsibilities.
2. **Decompose:** Group related behaviors and data by responsibility.
3. **Design:** Propose the classes needed to implement the feature.
4. **Validate:** Check each proposed class against SRP and identify potential reasons for change.
5. **Present:** Summarize the proposed classes, their responsibilities, and their relationships using the expected deliverables below.
6. **Implement:** Create the classes only after the design has been evaluated against SRP.

## Constraints

- Do not create or modify classes before analyzing their responsibilities.
- Do not begin implementation while significant responsibility boundaries remain unclear.
- Do not introduce unrelated architectural changes.
- Do not invent additional requirements or business responsibilities.
- Follow existing project conventions and preserve compatibility with the established architecture.
- If a responsibility cannot be assigned confidently, explain the ambiguity before proceeding.

## Expected deliverables

Before creating classes, provide:

1. A list of the responsibilities identified from the requirements.
2. The proposed classes and the single responsibility of each.
3. The behaviors and data assigned to each class.
4. The dependencies and collaboration between the proposed classes.
5. A brief explanation of how the design complies with SRP.
6. Any unresolved design questions that could affect class boundaries.

## Rules

| ID  | Rule | Check |
| --- | ---- | ----- |
| R1  | The skill is discoverable, kebab-case, and its description says when to use it | static |
| R2  | Each proposed class has one clearly defined responsibility and one primary reason to change | static |
| R3  | SRP is applied from the beginning of the design, not by subsequent refactoring | static |
| R4  | Requirements and acceptance criteria are read and analyzed before any class is designed | static |
| R5  | Core business logic is distinguished from supporting concerns when they are separate responsibilities | static |
| R6  | Every proposed class states its purpose in one clear sentence with cohesive methods | static |
| R7  | Unrelated business concerns never share a class merely because they belong to the same feature | static |
| R8  | Responsibilities with independent reasons to change go to separate classes; related data and behavior stay together | static |
| R9  | Collaboration is defined before implementation, wired with dependency injection when appropriate | static |
| R10 | The design follows the project's existing architectural and package conventions | static |
| R11 | Overengineering is avoided: no class per method, no unnecessary abstractions, cohesion preferred | static |
| R12 | The workflow runs Analyze, Decompose, Design, Validate, Present, Implement in that order | static |
| R13 | No class is created or modified before its responsibilities are analyzed and the design is validated | static |
| R14 | Implementation does not start while significant responsibility boundaries remain unclear; ambiguity is explained first | static |
| R15 | No invented requirements and no unrelated architectural changes | static |
| R16 | Before creating classes the six expected deliverables are provided | static |
| R17 | Other design principles and refactoring-only work are out of scope | static |
| R18 | The skill ships a Rules table covering every spec ID and a README | static |
| R19 | A feature request is answered with the SRP design deliverables before any class is written | agent-evaluation |
| R20 | A request to split a cohesive class into one class per method is pushed back as overengineering | agent-evaluation |

## Acceptance criteria

- [ ] The responsibilities and class boundaries have been analyzed and justified before implementation.
- [ ] Every proposed class has a cohesive purpose and one primary reason to change.
- [ ] Unrelated reasons for change are separated where appropriate.
- [ ] The design avoids unnecessary complexity: no class per method, no needless abstraction.
- [ ] The six expected deliverables were presented before any class was created.
- [ ] Ambiguous responsibilities were explained before proceeding, and no requirement was invented.
