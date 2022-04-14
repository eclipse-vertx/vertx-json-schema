package io.vertx.json.schema;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.Draft;
import io.vertx.json.schema.validator.ValidationResult;
import io.vertx.json.schema.validator.impl.AbstractSchema;
import io.vertx.json.schema.validator.impl.URL;
import io.vertx.json.schema.validator.impl.ValidatorImpl;
import io.vertx.junit5.Timeout;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TCKTest {

  private static final JsonObject UNSUPPORTED;
  private static final JsonObject TCK;

  private static final Map<String, io.vertx.json.schema.validator.Schema<?>> remotesLookup = new HashMap<>();
  private static final Map<String, io.vertx.json.schema.validator.Schema<?>> metaLookup = new HashMap<>();

  static {
    try {
      UNSUPPORTED = new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src", "test", "resources", "unsupported-tck-tests.json"))));
      TCK = new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src", "test", "resources", "test-suite-tck.json"))));

      // load the remotes
      for (Object o : TCK.getJsonArray("remotes")) {
        JsonObject el = (JsonObject) o;
        Object s = el.getValue("value");
        String name = el.getString("name");
        if (s instanceof JsonObject) {
          ValidatorImpl.dereference(io.vertx.json.schema.validator.Schema.fromJson((JsonObject) s), remotesLookup, new URL(name), "");
        }
        if (s instanceof Boolean) {
          ValidatorImpl.dereference(io.vertx.json.schema.validator.Schema.fromBoolean((Boolean) s), remotesLookup, new URL(name), "");
        }
      }

      // load the meta schemas
      String[] ids = new String[]{
        "http://json-schema.org/draft-04/schema",
        "http://json-schema.org/draft-07/schema",
        "https://json-schema.org/draft/2019-09/schema",
        "https://json-schema.org/draft/2019-09/meta/core",
        "https://json-schema.org/draft/2019-09/meta/applicator",
        "https://json-schema.org/draft/2019-09/meta/validation",
        "https://json-schema.org/draft/2019-09/meta/meta-data",
        "https://json-schema.org/draft/2019-09/meta/format",
        "https://json-schema.org/draft/2019-09/meta/content",
        "https://json-schema.org/draft/2020-12/schema",
        "https://json-schema.org/draft/2020-12/meta/core",
        "https://json-schema.org/draft/2020-12/meta/applicator",
        "https://json-schema.org/draft/2020-12/meta/validation",
        "https://json-schema.org/draft/2020-12/meta/meta-data",
        "https://json-schema.org/draft/2020-12/meta/format-annotation",
        "https://json-schema.org/draft/2020-12/meta/content",
        "https://json-schema.org/draft/2020-12/meta/unevaluated"
      };

      for (String meta : ids) {
        JsonObject json = new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src", "test", "resources", meta.substring(meta.indexOf("://") + 3)))));
        ValidatorImpl.dereference(io.vertx.json.schema.validator.Schema.fromJson(json), metaLookup, new URL(meta), "");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Stream<Arguments> buildParameters() {

    List<Arguments> tests = new ArrayList<>();

    for (Object o : TCK.getJsonArray("suites")) {
      JsonObject el = (JsonObject) o;
      for (Object o1 : el.getJsonArray("value")) {
        JsonObject value = (JsonObject) o1;
        for (Object o2 : value.getJsonArray("tests")) {
          JsonObject test = (JsonObject) o2;

          tests.add(() -> new Object[]{
            Draft.from(el.getString("draft")),
            el.getString("name"),
            value.getString("description"),
            test.getString("description"),
            value,
            test
          });
        }
      }
    }

    return tests.stream();
  }

  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  @ParameterizedTest(name = "{1}/{2}/{3}")
  @MethodSource("buildParameters")
  public void test(Draft draft, String suiteName, String suiteDescription, String testDescription, JsonObject value, JsonObject test) {
    assumeFalse(isUnsupportedTest(suiteName, suiteDescription, testDescription));

    final Map<String, io.vertx.json.schema.validator.Schema<?>> schemaLookup = ValidatorImpl.dereference(AbstractSchema.from(value.getValue("schema")), new HashMap<>(), new URL("https://vertx.io"), "");

    final Map<String, io.vertx.json.schema.validator.Schema<?>> lookup = new HashMap<>();

    lookup.putAll(metaLookup);
    lookup.putAll(remotesLookup);
    lookup.putAll(schemaLookup);

    try {
      ValidationResult result =
        ValidatorImpl
          .validate(test.getValue("data"), AbstractSchema.from(value.getValue("schema")), draft, lookup, false, null, "#", "#", new HashSet<>());

      if (result.valid() != test.getBoolean("valid")) {
        fail(testDescription);
      }
    } catch (RuntimeException e) {
      if (!test.getBoolean("valid")) {
        fail(testDescription);
      }
    }
  }

  private static boolean isUnsupportedTest(String suiteName, String suiteDescription, String testDescription) {
    if (UNSUPPORTED.containsKey(suiteName)) {
      JsonObject sub = UNSUPPORTED.getJsonObject(suiteName);
      if (sub.containsKey(suiteDescription)) {
        JsonObject sub2 = sub.getJsonObject(suiteDescription);
        return sub2.containsKey(testDescription);
      }
    }
    return false;
  }
}
