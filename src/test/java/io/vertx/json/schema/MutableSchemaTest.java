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
import io.vertx.json.schema.asserts.MyAssertions;
import io.vertx.json.schema.common.SchemaParserInternal;
import io.vertx.json.schema.common.SchemaRouterImpl;
import io.vertx.json.schema.draft7.Draft7SchemaParser;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(VertxExtension.class)
public class MutableSchemaTest {

  @Test
  public void schemaMustBeSyncBeforeValidation(Vertx vertx) throws IOException {
    URI u = TestUtils.buildBaseUri("mutable_schema_test", "sync_1.json");
    JsonObject obj = TestUtils.loadJson(u);
    Schema schema = Draft7SchemaParser.parse(vertx, obj, u);

    MyAssertions.assertThat(schema).isSync();
    assertThatCode(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
      .doesNotThrowAnyException();
    assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", 0)))
      .isInstanceOf(ValidationException.class);
  }

  @Test
  public void testRefToDependencies(Vertx vertx, VertxTestContext testContext) throws Exception {
    URI u = TestUtils.buildBaseUri("mutable_schema_test", "async_ref_1.json");
    JsonObject obj = TestUtils.loadJson(u);
    Schema schema = Draft7SchemaParser.parse(vertx, obj, u);

    MyAssertions.assertThat(schema).isAsync();

    assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
      .isInstanceOf(NoSyncValidationException.class);

    schema
      .validateAsync(new JsonObject().put("hello", "a"))
      .onComplete(testContext.succeeding(r -> {
        testContext.verify(() -> {
          MyAssertions.assertThat(schema)
            .isSync();
          assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", 0)))
            .isInstanceOf(ValidationException.class);
        });
        testContext.completeNow();
      }));
  }

  @Test
  public void testRefToDependenciesPreSolved(Vertx vertx, VertxTestContext testContext) throws Exception {
    URI u = TestUtils.buildBaseUri("mutable_schema_test", "async_ref_1.json");
    JsonObject obj = TestUtils.loadJson(u);
    SchemaRouterImpl router = (SchemaRouterImpl) SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser.create(router);
    Schema schema = parser.parse(obj, u);

    MyAssertions.assertThat(schema).isAsync();

    assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
      .isInstanceOf(NoSyncValidationException.class);

    router
      .solveAllSchemaReferences(schema)
      .onComplete(testContext.succeeding(s -> {
        testContext.verify(() -> {
          MyAssertions.assertThat(s).isSameAs(schema);
          MyAssertions.assertThat(schema)
            .isSync();
          assertThatCode(() -> schema.validateSync(new JsonObject().put("hello", "francesco")))
            .doesNotThrowAnyException();
          assertThatThrownBy(() -> schema.validateSync(new JsonObject().put("hello", 0)))
            .isInstanceOf(ValidationException.class);
          MyAssertions.assertThat(router)
            .containsCachedSchemasWithXIds("main", "hello_prop", "hello_def");
        });
        testContext.completeNow();
      }));
  }

  @Test
  public void testCircularRefs(Vertx vertx, VertxTestContext testContext) throws Exception {
    URI u = TestUtils.buildBaseUri("mutable_schema_test", "circular.json");
    JsonObject obj = TestUtils.loadJson(u);
    SchemaRouterImpl router = (SchemaRouterImpl) SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft7SchemaParser.create(router);
    Schema schema = parser.parse(obj, u);

    MyAssertions.assertThat(schema).isAsync();

    assertThatThrownBy(() -> schema.validateSync(new JsonObject()))
      .isInstanceOf(NoSyncValidationException.class);

    router
      .solveAllSchemaReferences(schema)
      .onComplete(testContext.succeeding(s -> {
        testContext.verify(() -> {
          MyAssertions.assertThat(s).isSameAs(schema);
          MyAssertions.assertThat(schema)
            .isSync();
          assertThatCode(() -> schema.validateSync(new JsonObject()
            .put("a", new JsonObject())
            .put("c", new JsonArray().add(1).add(new JsonObject()))
            .put("b", new JsonObject().put("sub-a", 1).put("sub-b", new JsonArray().add(1).add(new JsonObject().put("c", new JsonArray().add(1)))))
          )).doesNotThrowAnyException();
          assertThatThrownBy(() -> schema.validateSync(new JsonObject()
            .put("a", new JsonObject())
            .put("c", new JsonArray().add(1).add(new JsonObject()))
            .put("b", new JsonObject().put("sub-a", 1).put("sub-b", new JsonArray().add(1).add(new JsonObject().put("c", new JsonArray().addNull()))))
          )).isInstanceOf(ValidationException.class);
          MyAssertions.assertThat(router)
            .containsCachedSchemasWithXIds("main", "a", "b", "c", "c-items", "sub", "sub-a", "sub-b", "sub-b-items");
        });
        testContext.completeNow();
      }));
  }

}
