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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.asserts.MyAssertions;
import io.vertx.json.schema.common.SchemaInternal;
import io.vertx.json.schema.common.SchemaParserInternal;
import io.vertx.json.schema.common.SchemaRouterImpl;
import io.vertx.json.schema.draft7.Draft7SchemaParser;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;

import static io.vertx.json.schema.TestUtils.buildBaseUri;
import static io.vertx.json.schema.TestUtils.loadJson;
import static io.vertx.json.schema.asserts.MyAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(VertxExtension.class)
public class DefaultValuesApplyTest {

  @Test
  public void simpleDefault(Vertx vertx) throws IOException {
    URI u = buildBaseUri("default_test", "simple_default.json");
    JsonObject obj = loadJson(u);
    SchemaInternal schema = (SchemaInternal) Draft7SchemaParser.parse(vertx, obj, u);

    MyAssertions.assertThat(schema)
      .isSync();
    assertThatCode(() -> schema.validateSync(new JsonObject().put("a", "francesco")))
      .doesNotThrowAnyException();

    JsonObject objToApplyDefaults = new JsonObject();
    assertThatCode(() -> schema.getOrApplyDefaultSync(objToApplyDefaults)).doesNotThrowAnyException();
    assertThatJson(objToApplyDefaults)
      .extracting(JsonPointer.create().append("a"))
      .isEqualTo("hello");
  }

  @Test
  public void nested(Vertx vertx) throws IOException {
    URI u = buildBaseUri("default_test", "nested.json");
    JsonObject obj = loadJson(u);
    SchemaInternal schema = (SchemaInternal) Draft7SchemaParser.parse(vertx, obj, u);

    MyAssertions.assertThat(schema)
      .isSync();
    assertThatCode(() -> schema.validateSync(new JsonObject().put("a", new JsonObject())))
      .doesNotThrowAnyException();

    JsonObject objToApplyDefaults = new JsonObject().put("a", new JsonObject());
    assertThatCode(() -> schema.getOrApplyDefaultSync(objToApplyDefaults)).doesNotThrowAnyException();
    assertThatJson(objToApplyDefaults)
      .extracting(JsonPointer.create().append("b"))
      .isEqualTo("b_default");
    assertThatJson(objToApplyDefaults)
      .extracting(JsonPointer.create().append("a").append("c"))
      .isEqualTo(0);
  }

  @Test
  public void arrays(Vertx vertx) throws IOException {
    URI u = buildBaseUri("default_test", "arrays.json");
    JsonObject obj = loadJson(u);
    SchemaInternal schema = (SchemaInternal) Draft7SchemaParser.parse(vertx, obj, u);

    MyAssertions.assertThat(schema)
      .isSync();
    assertThatCode(() -> schema.validateSync(new JsonObject().put("a", new JsonArray().add(new JsonObject().put("inner", 1)))))
      .doesNotThrowAnyException();

    JsonObject objToApplyDefaults = new JsonObject().put("a", new JsonArray().add(new JsonObject()).add(new JsonObject()));
    assertThatCode(() -> schema.getOrApplyDefaultSync(objToApplyDefaults)).doesNotThrowAnyException();
    assertThatJson(objToApplyDefaults)
      .extracting(JsonPointer.create().append("a").append("0").append("inner"))
      .isEqualTo(0);
    assertThatJson(objToApplyDefaults)
      .extracting(JsonPointer.create().append("a").append("1").append("inner"))
      .isEqualTo(0);
  }

  @Test
  public void ref(Vertx vertx, VertxTestContext testContext) throws IOException {
    URI u = buildBaseUri("default_test", "ref.json");
    JsonObject obj = loadJson(u);
    SchemaRouterImpl router = (SchemaRouterImpl) SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser.create(router);
    SchemaInternal schema = parser.parse(obj, u);

    router
      .solveAllSchemaReferences(schema)
      .onComplete(testContext.succeeding(s -> {
        testContext.verify(() -> {
          assertThatCode(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
            .doesNotThrowAnyException();

          JsonObject objToApplyDefaults = new JsonObject();
          assertThatCode(() -> schema.getOrApplyDefaultSync(objToApplyDefaults)).doesNotThrowAnyException();
          assertThatJson(objToApplyDefaults)
            .extracting(JsonPointer.create().append("hello"))
            .isEqualTo("world");
        });
        testContext.completeNow();
      }));
  }

  @Test
  public void refWithoutPreResolution(Vertx vertx, VertxTestContext testContext) throws IOException {
    URI u = buildBaseUri("default_test", "ref.json");
    JsonObject obj = loadJson(u);
    SchemaRouterImpl router = (SchemaRouterImpl) SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser.create(router);
    SchemaInternal schema = parser.parse(obj, u);

    JsonObject objToApplyDefaults = new JsonObject();
    testContext.assertComplete(
      schema.getOrApplyDefaultAsync(objToApplyDefaults)
    ).onSuccess(defaulted ->
      testContext.verify(() -> {
        assertThat(defaulted)
          .isSameAs(objToApplyDefaults);

        assertThatJson(objToApplyDefaults)
          .extracting(JsonPointer.create().append("hello"))
          .isEqualTo("world");

        testContext.completeNow();
      })
    );
  }

  @Test
  public void circularRef(Vertx vertx, VertxTestContext testContext) throws IOException {
    URI u = buildBaseUri("default_test", "circular_ref.json");
    JsonObject obj = loadJson(u);
    SchemaRouterImpl router = (SchemaRouterImpl) SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser.create(router);
    SchemaInternal schema = parser.parse(obj, u);

    router
      .solveAllSchemaReferences(schema)
      .onComplete(testContext.succeeding(s -> {
        testContext.verify(() -> {
          assertThatCode(() -> schema.validateSync(new JsonObject().put("hello", new JsonObject())))
            .doesNotThrowAnyException();

          JsonObject objToApplyDefaults = new JsonObject().put("hello", new JsonObject());
          assertThatCode(() -> schema.getOrApplyDefaultAsync(objToApplyDefaults)).doesNotThrowAnyException();
          assertThatJson(objToApplyDefaults)
            .extracting(JsonPointer.create().append("hello").append("name"))
            .isEqualTo("world");
          assertThatJson(objToApplyDefaults)
            .extracting(JsonPointer.create().append("hello").append("and"))
            .isEqualTo(new JsonObject().put("hello", new JsonObject().put("name", "francesco")));
        });
        testContext.completeNow();
      }));
  }

}
