---
name: writing-tests
description: Patterns and conventions for writing tests in the vertx-json-schema project
---

# Writing Tests - vertx-json-schema

## Test Framework

- **JUnit 5** (Jupiter) - version managed by parent POM
- **AssertJ** (3.x) - fluent assertions
- **Mockito** (5.x) - mocking with `mockito-junit-jupiter`
- **vertx-junit5** - `VertxExtension` for tests needing a `Vertx` instance

## Test Patterns

### Pattern 1: Simple Test (no Vert.x instance needed)

Most tests in this project are simple and synchronous. Use this when testing the `Validator` or `SchemaRepository` API directly:

```java
package io.vertx.tests;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MyFeatureTest {

  @Test
  public void testSomething() {
    Validator validator = Validator.create(
      JsonSchema.of(new JsonObject().put("type", "number")),
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.io")
        .setDraft(Draft.DRAFT202012));

    OutputUnit result = validator.validate(42);
    assertThat(result.getValid()).isTrue();
  }
}
```

### Pattern 2: Test with Vert.x instance

Use `@ExtendWith(VertxExtension.class)` when the test needs a `Vertx` instance (e.g. for file system access to load meta-schemas):

```java
package io.vertx.tests;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class MyVertxTest {

  @Test
  public void testWithMetaSchemaValidation(Vertx vertx) {
    SchemaRepository repository = SchemaRepository.create(
      new JsonSchemaOptions()
        .setDraft(Draft.DRAFT202012)
        .setBaseUri("app://")
        .setOutputFormat(OutputFormat.Basic));

    OutputUnit result = repository
      .preloadMetaSchema(vertx.fileSystem())
      .validator("https://json-schema.org/draft/2020-12/schema")
      .validate(new JsonObject("{\"type\": \"object\"}"));

    assertThat(result.getValid()).isTrue();
  }
}
```

### Pattern 3: Parameterized Test

Used for data-driven tests, most notably the TCK. Use `@ParameterizedTest` with `@MethodSource`:

```java
package io.vertx.tests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class MyParameterizedTest {

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of("number", 42, true),
      Arguments.of("number", "hello", false),
      Arguments.of("string", "hello", true)
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  public void testValidation(String type, Object value, boolean expectedValid) {
    // ...
    assertThat(result.getValid()).isEqualTo(expectedValid);
  }
}
```

### Pattern 4: Tests with Mockito

Use Mockito for unit-testing implementation internals:

```java
package io.vertx.tests.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyImplTest {

  @Test
  void testSomething() {
    // Use Mockito.spy() or Mockito.mock() as needed
  }
}
```

## Test Location

| What you're testing | Package | Example |
|---|---|---|
| Public API (`Validator`, `SchemaRepository`, `JsonSchema`) | `io.vertx.tests` | `ValidatorTest.java` |
| Implementation internals (`impl/` package) | `io.vertx.tests.impl` | `SchemaRepositoryImplTest.java` |

## Assertions

Use **AssertJ** for all assertions:

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

// Boolean
assertThat(result.getValid()).isTrue();
assertThat(result.getValid()).isFalse();

// Equality
assertThat(result.getErrorType()).isEqualByComparingTo(OutputErrorType.INVALID_VALUE);

// Null
assertThat(result.getErrors()).isNull();
assertThat(result.getErrors()).isNotNull();

// Collections
assertThat(result.getErrors()).hasSize(3);
assertThat(result.getErrors()).isEmpty();

// Exceptions
import static org.junit.jupiter.api.Assertions.assertThrows;
assertThrows(SchemaException.class, () -> { /* ... */ });
```

## Test Data

- **JSON Schema test data**: `src/test/resources/` contains test schemas and fixture data
- **TCK data**: `src/test/resources/test-suite-tck.json` - the official JSON Schema Test Suite
- **Meta-schemas**: available from `src/main/resources/json-schema.org/` (loaded via `SchemaRepository.preloadMetaSchema()`)
- **Additional classpath**: `src/test/resources/ref_test/schemas.jar` is on the test classpath (configured in `pom.xml`)

## Test Requirements

- All new features **must** have tests
- All bug fixes **should** have a regression test
- Tests are **synchronous** - do not use `Future`, `Promise`, or async patterns in test code
- Use `@Timeout(value = 10, timeUnit = TimeUnit.SECONDS)` on tests that could hang

## Common Pitfalls

- **Do not use JUnit 4** (`org.junit.Test`, `@RunWith`). This project uses JUnit 5.
- **Do not use JUnit assertions** (`assertEquals`, `assertTrue`). Use AssertJ's `assertThat(...)` API.
- **Do not put tests in `io.vertx.json.schema`** package. Tests go in `io.vertx.tests` or `io.vertx.tests.impl`.
- **Do not edit generated files** in `src/main/generated/`.
