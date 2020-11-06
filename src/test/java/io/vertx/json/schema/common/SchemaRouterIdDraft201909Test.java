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
package io.vertx.json.schema.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.asserts.MyAssertions;
import io.vertx.json.schema.draft201909.Draft201909SchemaParser;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import static io.vertx.json.schema.TestUtils.buildBaseUri;
import static io.vertx.json.schema.TestUtils.loadJson;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchemaRouterIdDraft201909Test {

  public SchemaParserInternal parser;
  public SchemaRouter schemaRouter;

  @BeforeAll
  public void setUp(Vertx vertx) {
    schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    parser = Draft201909SchemaParser.create(schemaRouter);
  }

  /*

    From https://json-schema.org/draft/2019-09/json-schema-core.html#rfc.appendix.A

    # (document root)

        canonical absolute-URI (and also base URI)
            https://example.com/root.json
        canonical URI with pointer fragment
            https://example.com/root.json#

    #/$defs/A

        base URI
            https://example.com/root.json
        canonical URI with plain fragment
            https://example.com/root.json#foo
        canonical URI with pointer fragment
            https://example.com/root.json#/$defs/A

    #/$defs/B

        base URI
            https://example.com/other.json
        canonical URI with pointer fragment
            https://example.com/other.json#
        non-canonical URI with fragment relative to root.json
            https://example.com/root.json#/$defs/B

    #/$defs/B/$defs/X

        base URI
            https://example.com/other.json
        canonical URI with plain fragment
            https://example.com/other.json#bar
        canonical URI with pointer fragment
            https://example.com/other.json#/$defs/X
        non-canonical URI with fragment relative to root.json
            https://example.com/root.json#/$defs/B/$defs/X

    #/$defs/B/$defs/Y

        base URI
            https://example.com/t/inner.json
        canonical URI with plain fragment
            https://example.com/t/inner.json#bar
        canonical URI with pointer fragment
            https://example.com/t/inner.json#
        non-canonical URI with fragment relative to other.json
            https://example.com/other.json#/$defs/Y
        non-canonical URI with fragment relative to root.json
            https://example.com/root.json#/$defs/B/$defs/Y

    #/$defs/C

        base URI
            urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f
        canonical URI with pointer fragment
            urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f#
        non-canonical URI with fragment relative to root.json
            https://example.com/root.json#/$defs/C

   */

  @Test
  public void testDraft201909AppendixA() throws Exception {
    URI baseURI = buildBaseUri("id_test", "rfc_201909_appendix_a.json");
    JsonPointer basePointer = JsonPointer.fromURI(baseURI);
    JsonObject baseSchemaJson = loadJson(baseURI);
    parser.parse(baseSchemaJson, baseURI);

    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema(JsonPointer.create(), basePointer, parser)
      .hasXIdEqualsTo("root");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json", basePointer, parser)
      .hasXIdEqualsTo("root");

    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("#/$defs/A", basePointer, parser)
      .hasXIdEqualsTo("A");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("#foo", basePointer, parser)
      .hasXIdEqualsTo("A");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#/$defs/A", basePointer, parser)
      .hasXIdEqualsTo("A");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#foo", basePointer, parser)
      .hasXIdEqualsTo("A");

    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("#/$defs/B", basePointer, parser)
      .hasXIdEqualsTo("B");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/other.json", basePointer, parser)
      .hasXIdEqualsTo("B");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema(JsonPointer.create(), JsonPointer.fromURI(URI.create("https://example.com/other.json")), parser)
      .hasXIdEqualsTo("B");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#/$defs/B", basePointer, parser)
      .hasXIdEqualsTo("B");

    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("#/$defs/B/$defs/X", basePointer, parser)
      .hasXIdEqualsTo("X");
    MyAssertions.assertThat(schemaRouter)
      .cannotResolveSchema("#bar", basePointer, parser);
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("#bar", JsonPointer.fromURI(URI.create("https://example.com/other.json")), parser)
      .hasXIdEqualsTo("X");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/other.json#bar", basePointer, parser)
      .hasXIdEqualsTo("X");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/other.json#/$defs/X", basePointer, parser)
      .hasXIdEqualsTo("X");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#/$defs/B/$defs/X", basePointer, parser)
      .hasXIdEqualsTo("X");

    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("#/$defs/B/$defs/Y", basePointer, parser)
      .hasXIdEqualsTo("Y");
    MyAssertions.assertThat(schemaRouter)
      .cannotResolveSchema("#bar", basePointer, parser);
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("#bar", JsonPointer.fromURI(URI.create("https://example.com/t/inner.json")), parser)
      .hasXIdEqualsTo("Y");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/t/inner.json#bar", basePointer, parser)
      .hasXIdEqualsTo("Y");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema(JsonPointer.create(), JsonPointer.fromURI(URI.create("https://example.com/t/inner.json")), parser)
      .hasXIdEqualsTo("Y");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/other.json#/$defs/Y", basePointer, parser)
      .hasXIdEqualsTo("Y");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("https://example.com/root.json#/$defs/B/$defs/Y", basePointer, parser)
      .hasXIdEqualsTo("Y");

    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("#/$defs/C", basePointer, parser)
      .hasXIdEqualsTo("C");
    MyAssertions.assertThat(schemaRouter)
      .canResolveSchema("urn:uuid:ee564b8a-7a87-4125-8c96-e9f123d6766f", basePointer, parser)
      .hasXIdEqualsTo("C");
  }

}
