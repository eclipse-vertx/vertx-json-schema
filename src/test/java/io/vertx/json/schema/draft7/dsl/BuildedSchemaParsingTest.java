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
package io.vertx.json.schema.draft7.dsl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.asserts.MyAssertions;
import io.vertx.json.schema.draft7.Draft7SchemaParser;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import static io.vertx.json.schema.common.dsl.Schemas.*;
import static io.vertx.json.schema.draft7.dsl.Keywords.exclusiveMaximum;
import static io.vertx.json.schema.draft7.dsl.Keywords.multipleOf;

@ExtendWith(VertxExtension.class)
public class BuildedSchemaParsingTest {

  SchemaRouter router;
  SchemaParser parser;

  @BeforeEach
  public void setUp(Vertx vertx) {
    router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    parser = Draft7SchemaParser.create(router);
  }

  @Test
  public void testCircularTreeDeclaration(VertxTestContext testContext) {
    Schema schema =
      objectSchema()
        .alias("root_object")
        .requiredProperty("value",
          intSchema()
            .with(exclusiveMaximum(20d), multipleOf(2d))
        )
        .property("leftChild", refToAlias("root_object"))
        .property("rightChild", refToAlias("root_object"))
        .build(parser);
    testContext.assertComplete(schema.validateAsync(
      new JsonObject().put("value", 6).put("leftChild", new JsonObject().put("value", 2))
    )).onComplete(v -> testContext.completeNow());
  }

  @Test
  public void testRelativeFileResolution(VertxTestContext testContext) {
    Schema schema = ref(JsonPointer.fromURI(URI.create("ref_test/sample.json"))).build(parser);

    testContext.assertComplete(schema.validateAsync("")).onComplete(v -> {
      testContext.verify(() -> {
        MyAssertions.assertThat(router).containsOnlyOneCachedSchemaWithXId("main");
        MyAssertions.assertThat(router).containsOnlyOneCachedSchemaWithXId("sub1");
      });
      testContext.completeNow();
    });
  }

}
