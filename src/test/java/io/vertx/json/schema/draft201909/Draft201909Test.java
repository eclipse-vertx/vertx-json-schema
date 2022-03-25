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
package io.vertx.json.schema.draft201909;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.SchemaParserInternal;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static io.vertx.json.schema.TestUtils.buildBaseUri;
import static io.vertx.json.schema.TestUtils.loadJson;
import static io.vertx.json.schema.asserts.MyAssertions.assertThat;

@ExtendWith(VertxExtension.class)
public class Draft201909Test {

  //TODO test ignoreFormatKeyword

  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  @Test
  public void testAppendixC(Vertx vertx) throws IOException {
    URI treeSchemaURI = buildBaseUri("ref_test", "draft201909_appendix_c", "tree.json");
    JsonObject treeSchemaUnparsed = loadJson(treeSchemaURI);

    URI strictTreeSchemaURI = buildBaseUri("ref_test", "draft201909_appendix_c", "strict-tree.json");
    JsonObject strictTreeSchemaUnparsed = loadJson(strictTreeSchemaURI);

    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft201909SchemaParser.create(router);

    Schema treeSchema = parser.parse(treeSchemaUnparsed, treeSchemaURI);
    Schema strictTreeSchema = parser.parse(strictTreeSchemaUnparsed, strictTreeSchemaURI);

    JsonObject value = new JsonObject("{\"children\": [ { \"daat\": 1 } ]}");

    assertThat(treeSchema)
      .validateAsyncSuccess(value);
    assertThat(strictTreeSchema)
      .validateAsyncFailure(value);
  }

  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  @Test
  public void testRefWithNeighbourKeywords(Vertx vertx) throws IOException {
    URI schemaURI = buildBaseUri("ref_test", "person_draft201909.json");
    JsonObject schemaUnparsed = loadJson(schemaURI);

    SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParserInternal parser = Draft201909SchemaParser.create(router);

    Schema schema = parser.parse(schemaUnparsed, schemaURI);

    assertThat(schema)
      .validateAsyncFailure(new JsonObject().put("id_card", 1));
    assertThat(schema)
      .validateAsyncFailure(new JsonObject().put("id_card", "ABC"));
    assertThat(schema)
      .validateAsyncSuccess(new JsonObject().put("id_card", "ABC").put("name", "Paolo").put("surname", "Rossi"))
      .isSync();
  }

}
