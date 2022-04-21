package io.vertx.json.schema;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.*;
import io.vertx.json.schema.validator.Schema;
import io.vertx.junit5.Timeout;
import org.junit.jupiter.api.BeforeAll;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TCKTest {

  private static final Properties UNSUPPORTED = new Properties();
  private static final JsonObject TCK;

  private static final Map<String, Schema> REMOTES = new HashMap<>();
  private static final Map<String, Schema> METAS = new HashMap<>();

  static {
    try {
      UNSUPPORTED.load(TCKTest.class.getResourceAsStream("/unsupported-tck-tests.properties"));
      TCK = new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src", "test", "resources", "test-suite-tck.json"))));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeAll
  public static void loadMetas() throws IOException {
    // load the meta schemas
    String[] ids = new String[] {
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

    for (String id : ids) {
      String[] relative = id.substring(id.indexOf("://") + 3).split("/");
      String[] segments = new String[relative.length + 2];
      segments[0] = "test";
      segments[1] = "resources";
      System.arraycopy(relative, 0, segments, 2, relative.length);
      METAS.put(id, Schema.of(new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src", segments))))));
    }
  }

  @BeforeAll
  public static void loadRemotes() {
    // load the remotes
    for (Object o : TCK.getJsonArray("remotes")) {
      JsonObject el = (JsonObject) o;
      Object s = el.getValue("value");
      String name = el.getString("name");
      if (s instanceof JsonObject) {
        REMOTES.put(name, Schema.of((JsonObject) s));
        continue;
      }
      if (s instanceof Boolean) {
        REMOTES.put(name, Schema.of((Boolean) s));
        continue;
      }
      fail("remotes contains unknown kind of schema");
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

    return tests
      .stream()
      .sorted((Arguments arg1, Arguments arg2) -> {
        Draft d1 = (Draft) arg1.get()[0];
        Draft d2 = (Draft) arg2.get()[0];
        return Integer.compare(d1.ordinal(), d2.ordinal());
      });
  }

  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  @ParameterizedTest(name = "{1}/{2}/{3}")
  @MethodSource("buildParameters")
  public void test(Draft draft, String suiteName, String suiteDescription, String testDescription, JsonObject value, JsonObject test) {
    final boolean unsupported = UNSUPPORTED.containsKey(suiteName + "/" + suiteDescription + "/" + testDescription);

    try {
      Object rawSchema = value.getValue("schema");
      Schema testSchema = null;
      if (rawSchema instanceof JsonObject) {
        testSchema = Schema.of((JsonObject) rawSchema);
      }
      if (rawSchema instanceof Boolean) {
        testSchema = Schema.of((Boolean) rawSchema);
      }

      assertThat(testSchema).isNotNull();

      // setup the initial validator object
      final Validator validator = Validator.create(
        testSchema,
        new ValidatorOptions()
          .setDraft(draft)
          .setBaseUri("https://github.com/eclipse-vertx"));

      // load all meta schemas
      for (Map.Entry<String, Schema> kv : METAS.entrySet()) {
        validator.addSchema(kv.getKey(), kv.getValue());
      }

      // load all remote schemas
      for (Map.Entry<String, Schema> kv : REMOTES.entrySet()) {
        validator.addSchema(kv.getKey(), kv.getValue());
      }

      ValidationResult result =
        validator
          .validate(test.getValue("data"));

      if (result.valid() != test.getBoolean("valid")) {
        if (unsupported) {
          // this means we don't really support this and the validation failed, so we will ignore it for now
          assumeFalse(unsupported, testDescription);
        } else {
          fail(testDescription);
        }
      } else {
        if (unsupported) {
          fail("Test should be marked as supported : " + suiteName + "/" + suiteDescription + "/" + testDescription);
//          UNSUPPORTED.remove(suiteName + "/" + suiteDescription + "/" + testDescription);
        }
      }
    } catch (IllegalStateException e) {
      if (test.getBoolean("valid", false)) {
        if (unsupported) {
          // this means we don't really support this and the validation failed, so we will ignore it for now
          assumeFalse(unsupported, testDescription);
        } else {
          fail(testDescription);
        }
      }
    }
  }
//
//  @AfterAll
//  public static void updateProps() throws IOException {
//    UNSUPPORTED.store(Files.newOutputStream(new File("src/test/resources/unsupported-tck-tests.properties.1").toPath()), "Unsupported json-schema.org TCK tests (suiteName/suiteDescription/testDescription)");
//  }
}
