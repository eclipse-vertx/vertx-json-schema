package io.vertx.ext.json.schema;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.json.schema.common.SchemaRouterImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

  public static final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);
  public static final int SCHEMA_SERVER_PORT = 1234;

  private HttpServer schemaServer;

  private void startSchemaServer(Vertx vertx, Handler<AsyncResult<Void>> completion) {
    Router r = Router.router(vertx);
    r.route("/*")
      .produces("application/json")
      .handler(StaticHandler.create(getRemotesPath().toString()).setCachingEnabled(true));
    schemaServer = vertx.createHttpServer(new HttpServerOptions().setPort(SCHEMA_SERVER_PORT))
      .requestHandler(r)
      .listen(l -> completion.handle(Future.succeededFuture()));
  }

  private void stopSchemaServer(Handler<AsyncResult<Void>> completion) {
    if (schemaServer != null) {
      try {
        schemaServer.close((asyncResult) -> {
          completion.handle(Future.succeededFuture());
        });
      } catch (IllegalStateException e) { // Server is already open
        completion.handle(Future.succeededFuture());
      }
    }
  }

  @BeforeAll
  public void setUp(Vertx vertx, VertxTestContext testContext) {
    startSchemaServer(vertx, testContext.completing());
  }

  @AfterAll
  public void tearDown(VertxTestContext testContext) {
    stopSchemaServer(testContext.completing());
  }

  private Optional<Map.Entry<SchemaParser, Schema>> buildSchema(Vertx vertx, Object schema, String testName, String testFileName) {
    try {
      return Optional.of(buildSchemaFunction(vertx, schema, testFileName));
    } catch (Exception e) {
      fail("Something went wrong during schema initialization for test \"" + testName + "\"", e);
      return Optional.empty();
    }
  }

  private void validateSuccess(Schema schema, SchemaParser parser, Object obj, String testName, String testCaseName, VertxTestContext context) {
    schema.validateAsync(obj).setHandler(event -> {
      if (event.failed())
        context.verify(() -> fail(String.format("\"%s\" -> \"%s\" should be valid", testName, testCaseName), event.cause()));

      ((SchemaRouterImpl)parser.getSchemaRouter()).solveAllSchemaReferences(schema).setHandler(ar -> {
        context.verify(() -> {
          assertThat(ar.succeeded())
              .isTrue()
              .withFailMessage("Failed schema refs resolving with cause {}", ar.cause());
          assertThat(schema.isSync())
              .isTrue();
          assertThatCode(() -> schema.validateSync(obj))
              .as("\"%s\" -> \"%s\" should be valid", testName, testCaseName)
              .doesNotThrowAnyException();
        });
        context.completeNow();
      });
    });
  }

  private void validateFailure(Schema schema, SchemaParser parser, Object obj, String testName, String testCaseName, VertxTestContext context) {
    schema.validateAsync(obj).setHandler(event -> {
      if (event.succeeded())
        context.verify(() -> fail(String.format("\"%s\" -> \"%s\" should be invalid", testName, testCaseName)));
      else if (log.isDebugEnabled())
        log.debug(event.cause().toString());

      ((SchemaRouterImpl)parser.getSchemaRouter()).solveAllSchemaReferences(schema).setHandler(ar -> {
        context.verify(() -> {
          assertThat(ar.succeeded())
              .isTrue()
              .withFailMessage("Failed schema refs resolving with cause {}", ar.cause());
          assertThat(schema.isSync())
              .isTrue();
          assertThatExceptionOfType(ValidationException.class)
              .isThrownBy(() -> schema.validateSync(obj))
              .as("\"%s\" -> \"%s\" should be invalid", testName, testCaseName);
        });
        context.completeNow();
      });
    });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("buildParameters")
  public void test(String testName, String testFileName, JsonObject testObj, Vertx vertx, VertxTestContext context) {
    buildSchema(vertx, testObj.getValue("schema"), testName, testFileName)
        .ifPresent(t -> {
          for (Object tc : testObj.getJsonArray("tests").stream().collect(Collectors.toList())) {
            JsonObject testCase = (JsonObject) tc;
            if (testCase.getBoolean("valid"))
              validateSuccess(t.getValue(), t.getKey(), testCase.getValue("data"), testName, testCase.getString("description"), context);
            else
              validateFailure(t.getValue(), t.getKey(), testCase.getValue("data"), testName, testCase.getString("description"), context);
          }
        });
  }

  public Stream<Arguments> buildParameters() {
    return getTestFiles()
        .map(f -> new AbstractMap.SimpleImmutableEntry<>(f, getTckPath().resolve(f + ".json")))
        .map(p -> {
          try {
            return new AbstractMap.SimpleImmutableEntry<>(p.getKey(), Files.readAllLines(p.getValue(), Charset.forName("UTF8")));
          } catch (IOException e) {
            e.printStackTrace();
            return null;
          }
        })
        .filter(Objects::nonNull)
        .map(strings -> new AbstractMap.SimpleImmutableEntry<>(strings.getKey(), String.join("", strings.getValue())))
        .map(string -> new AbstractMap.SimpleImmutableEntry<>(string.getKey(), new JsonArray(string.getValue())))
        .flatMap(t -> t.getValue()
            .stream()
            .map(JsonObject.class::cast)
            .map(o -> arguments(t.getKey() + ": " + o.getString("description"), t.getKey(), o))
        );
  }

  public abstract Stream<String> getTestFiles();

  public abstract Map.Entry<SchemaParser, Schema> buildSchemaFunction(Vertx vertx, Object schema, String testFileName) throws URISyntaxException;

  public abstract Path getTckPath();

  public abstract Path getRemotesPath();
}
