/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.json.schema;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.SchemaParserInternal;
import io.vertx.json.schema.common.SchemaRouterImpl;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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
    schemaServer = vertx.createHttpServer(new HttpServerOptions().setPort(SCHEMA_SERVER_PORT))
      .requestHandler(req -> {
        String path = req.path().split(Pattern.quote("#"))[0];
        req.response()
          .putHeader("Content-type", "application/json")
          .sendFile(Paths.get(getRemotesPath().toString(), path).toString());
      })
      .listen(l -> completion.handle(Future.succeededFuture()));
  }

  private void stopSchemaServer(Handler<AsyncResult<Void>> completion) {
    try {
      schemaServer.close((asyncResult) -> {
        completion.handle(Future.succeededFuture());
      });
    } catch (IllegalStateException e) { // Server is already open
      completion.handle(Future.succeededFuture());
    }
  }

  @BeforeAll
  public void setUp(Vertx vertx, VertxTestContext testContext) {
    if (getRemotesPath() != null) {
      startSchemaServer(vertx, testContext.succeedingThenComplete());
    } else {
      testContext.completeNow();
    }
  }

  @AfterAll
  public void tearDown(VertxTestContext testContext) {
    if (schemaServer != null) {
      stopSchemaServer(testContext.succeedingThenComplete());
    } else {
      testContext.completeNow();
    }
  }

  private Map.Entry<SchemaParser, Schema> buildSchema(Vertx vertx, Object schema, String testName, String testFileName) {
    try {
      return buildSchemaFunction(vertx, schema, testFileName);
    } catch (Exception e) {
      fail("Something went wrong during schema initialization for test \"" + testName + "\"", e);
      return null;
    }
  }

  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  @ParameterizedTest(name = "{0}")
  @MethodSource("buildParameters")
  public void test(String testName, String testFileName, JsonObject testObj, Vertx vertx, VertxTestContext context) {
    Map.Entry<SchemaParser, Schema> t = buildSchema(vertx, testObj.getValue("schema"), testName, testFileName);
    for (Object tc : testObj.getJsonArray("tests").stream().collect(Collectors.toList())) {
      JsonObject testCase = (JsonObject) tc;
      if (testObj.getBoolean("skip", false)) {
        log.warn("Skipping test: " + testCase.getString("description"));
        context.completeNow();
      } else {
        if (testCase.getBoolean("valid"))
          validateSuccess(t.getValue(), t.getKey(), testCase.getValue("data"), testName, testCase.getString("description"), context);
        else
          validateFailure(t.getValue(), t.getKey(), testCase.getValue("data"), testName, testCase.getString("description"), context);
      }
    }
  }

  private void validateSuccess(Schema schema, SchemaParser parser, Object obj, String testName, String testCaseName, VertxTestContext context) {
    schema.validateAsync(obj).onComplete(event -> {
      if (event.failed())
        context.verify(() -> fail(String.format("\"%s\" -> \"%s\" should be valid", testName, testCaseName), event.cause()));

      if (skipSyncCheck(schema)) {
        context.completeNow();
        return;
      }

      ((SchemaRouterImpl) parser.getSchemaRouter()).resolveAllSchemas().onComplete(ar -> {
        context.verify(() -> {
          if (ar.failed()) {
            fail("Failed schema refs resolving with cause", ar.cause());
          }
          assertThat(schema.isSync())
            .as("Schema is sync")
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
    schema.validateAsync(obj).onComplete(event -> {
      if (event.succeeded())
        context.verify(() -> fail(String.format("\"%s\" -> \"%s\" should be invalid", testName, testCaseName)));
      else if (log.isDebugEnabled())
        log.debug(event.cause().toString());

      if (skipSyncCheck(schema)) {
        context.completeNow();
        return;
      }

      ((SchemaRouterImpl) parser.getSchemaRouter()).resolveAllSchemas().onComplete(ar -> {
        context.verify(() -> {
          if (ar.failed()) {
            fail("Failed schema refs resolving with cause", ar.cause());
          }
          assertThat(schema.isSync())
            .as("Schema is sync")
            .isTrue();
          assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(() -> schema.validateSync(obj))
            .as("\"%s\" -> \"%s\" should be invalid", testName, testCaseName);
        });
        context.completeNow();
      });
    });
  }

  public Stream<Arguments> buildParameters() {
    return getTestFiles()
      .map(f -> new SimpleImmutableEntry<>(f, getTckPath().resolve(f + ".json")))
      .map(p -> {
        try {
          return new SimpleImmutableEntry<>(p.getKey(), String.join("", Files.readAllLines(p.getValue(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      })
      .map(string -> new SimpleImmutableEntry<>(string.getKey(), new JsonArray(string.getValue())))
      .flatMap(t -> t.getValue()
        .stream()
        .map(JsonObject.class::cast)
        .map(o -> arguments(t.getKey() + ": " + o.getString("description"), t.getKey(), o))
      );
  }

  public abstract Stream<String> getTestFiles();

  public abstract SchemaParserInternal getSchemaParser(Vertx vertx);

  protected Map.Entry<SchemaParser, Schema> buildSchemaFunction(Vertx vertx, Object schema, String testFileName) throws URISyntaxException {
    SchemaParserInternal parser = getSchemaParser(vertx);
    Schema s = parser.parse(schema, Paths.get(this.getTckPath() + "/" + testFileName + ".json").toAbsolutePath().toUri());
    return new AbstractMap.SimpleImmutableEntry<>(parser, s);
  }

  ;

  public abstract Path getTckPath();

  public abstract Path getRemotesPath();

  public boolean skipSyncCheck(Schema schema) {
    return false;
  }
}
