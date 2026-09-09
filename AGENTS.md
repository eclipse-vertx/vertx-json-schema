# Agent Guidelines - vertx-json-schema

Instructions for AI coding agents working in this repository.
Checkout relevant `.agents/skills/` to accomplish specific tasks.

## Build & Verify Commands

```bash
mvn test-compile         # compile code and tests
mvn test                 # run all tests
mvn spotless:check       # verify formatting
mvn spotless:apply       # auto-fix formatting
```

## Project Structure

This is a **single-module** Maven project implementing the [JSON Schema](https://json-schema.org/) specification for Eclipse Vert.x. It supports drafts 4, 7, 2019-09, and 2020-12.

### Source Layout

- `src/main/java/`: Public API interfaces and implementation classes
  - `io/vertx/json/schema/`: Public API (interfaces, `@VertxGen` annotated)
  - `io/vertx/json/schema/common/`: Shared utilities
  - `io/vertx/json/schema/common/dsl/`: DSL builders for constructing schemas programmatically
  - `io/vertx/json/schema/impl/`: Implementation classes (not exported)
- `src/main/generated/`: Generated sources (vertx-codegen) - do not edit
- `src/main/resources/json-schema.org/`: Bundled meta-schemas (draft-04, draft-07, 2019-09, 2020-12)
- `src/test/java/io/vertx/tests/`: Test classes
- `src/test/java/io/vertx/tests/impl/`: Tests for implementation internals
- `src/test/resources/`: Test schemas and TCK data
- `module-info.java`: Defines module exports and dependencies

### Key API Types

- `JsonSchema` - Represents a JSON Schema document
- `Validator` - Validates input against a schema
- `SchemaRepository` - Manages multiple schemas for reuse and cross-referencing
- `OutputUnit` - Represents validation results
- `Draft` - Enum for supported JSON Schema drafts

## General Coding Rules

These rules apply when **writing or modifying code**. Code review is the checkpoint where compliance is verified.

### Code Style

- **Indentation**: 2 spaces (see `.editorconfig`)
- **Line endings**: LF
- **Charset**: UTF-8
- Code formatting is enforced by **Spotless** (defined in parent POM `vertx5-parent`)
- Always run `mvn spotless:check` before submitting, and `mvn spotless:apply` to fix formatting

### Logging

In production code, use the Vert.x internal logger, never SLF4J, Log4j, or `java.util.logging` directly.

```java
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
```

### API Design

- Public contracts are interfaces in the top package (`io.vertx.json.schema`)
- Implementations go in `impl/` subpackage
- Annotate public API interfaces and methods with `@VertxGen` for code-generation support
- Builder/fluent methods are annotated with `@Fluent`
- Expose construction via static factory methods, not constructors (e.g. `Validator.create(...)`, `SchemaRepository.create(...)`, `JsonSchema.of(...)`)

### Module Boundaries

`module-info.java` governs exports.
Internal packages are exported only to their corresponding test modules, do not widen exports without discussion.
Test module descriptors (`src/test/java/module-info.java`) can be modified freely, e.g. to add a `requires` for a new dependency used in tests.

### Copyright Header

New Java files must include the dual-license header matching existing files:

```java
/*
 * Copyright (c) 2011-2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
```

The range is always `2011-[current year]`.

## Testing Guidelines

For comprehensive testing patterns and examples, see `.agents/skills/writing-tests/SKILL.md`.

### Test Framework

- Use **JUnit 5** (Jupiter) for all tests
- **AssertJ** (`assertThat(...)`) for fluent assertions - prefer over JUnit assertions
- **Mockito** (5.x) with `mockito-junit-jupiter` for mocking
- Tests that need a `Vertx` instance use **VertxExtension** (`@ExtendWith(VertxExtension.class)`) and accept `Vertx` as a parameter

### Test Patterns

```java
public class MyTest {

  @Test
  public void testSomething() {
    Validator validator = Validator.create(
      JsonSchema.of(new JsonObject().put("type", "number")),
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.io")
        .setDraft(Draft.DRAFT202012));

    assertThat(validator.validate(42).getValid()).isTrue();
  }
}
```

### Test Location

- Test classes go in `io.vertx.tests` package
- Tests for implementation internals go in `io.vertx.tests.impl` package

### Running Tests

```bash
mvn test                           # Run all tests
mvn test -Dtest=MyTest             # Run specific test class
mvn test -Dtest=MyTest#testMethod  # Run specific test method
```

### Test Requirements

- All new features must include tests
- All bug fixes should include a regression test
- Tests are **synchronous** - do not use `Future`, `Promise`, or async patterns in test code

## Development Workflow

### Incremental Development

When making changes:
1. Compile frequently: `mvn test-compile`
2. Run affected tests: `mvn test -Dtest=<TestClass>`
3. Verify formatting: `mvn spotless:check`
4. Run full build before PR: `mvn clean verify`

## Specialized Skills

When performing specific tasks, read the relevant skill file for detailed guidance:

- **Writing tests** - Read `.agents/skills/writing-tests/SKILL.md` when creating or modifying tests

## Contribution Process

- All commits must be signed off: `git commit -s` (DCO)
- Commit messages should end with: `Assisted-by: [Provider] [Model-Family] ([Version/ID])` (replace placeholders)
- Contributors must have signed the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php)

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full contribution workflow.

## Code Review Guidelines

### Verify

- General coding rules above are followed
- Test coverage is present; tests use JUnit 5 and AssertJ
- No breaking changes to public interfaces without prior discussion

### Do Not Comment On

- Patterns already used consistently throughout the codebase
