package io.vertx.json.schema;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.impl.URL;
import io.vertx.json.schema.validator.impl.ValidatorImpl;
import io.vertx.junit5.RunTestOnContext;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TCKTest {

  private static final JsonObject UNSUPPORTED;
  private static final JsonObject TCK;

  private static Map<String, io.vertx.json.schema.validator.Schema<?>> remotesLookup = new HashMap<>();

  static {
    try {
      UNSUPPORTED = new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src", "test", "resources", "unsupported-tck-tests.json"))));
      TCK = new JsonObject(Buffer.buffer(Files.readAllBytes(Paths.get("src", "test", "resources", "test-suite-tck.json"))));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

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
  }

  @RegisterExtension
  RunTestOnContext rtoc = new RunTestOnContext();

  public Stream<Arguments> buildParameters() {

    List<Arguments> tests = new ArrayList<>();

    for (Object o : TCK.getJsonArray("suites")) {
      JsonObject el = (JsonObject) o;
      for (Object o1 : el.getJsonArray("value")) {
        JsonObject value = (JsonObject) o1;
        for (Object o2 : value.getJsonArray("tests")) {
          JsonObject test = (JsonObject) o2;

          tests.add(() -> new Object[]{
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
  @ParameterizedTest(name = "{0} - {1} - {2}")
  @MethodSource("buildParameters")
  public void test(String suiteName, String suiteDescription, String testDescription, Object value, Object test, VertxTestContext should) {
    assumeFalse(isUnsupportedTest(suiteName, suiteDescription, testDescription));

    assertThat(value).isNotNull();
    assertThat(test).isNotNull();

    should.failNow("Not Implemented");
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
