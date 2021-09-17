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
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.asserts.MyAssertions;
import io.vertx.json.schema.openapi3.OpenAPI3SchemaParser;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static io.vertx.json.schema.TestUtils.buildBaseUri;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class SchemaRouterLocalRefTest {

  private VertxInternal vertx;

  public SchemaParserInternal parser;
  public SchemaRouter router;

  @BeforeEach
  public void setUp(Vertx vertx) {
    this.vertx = (VertxInternal) vertx;
    router = SchemaRouter.create(vertx, new SchemaRouterOptions());
    parser = OpenAPI3SchemaParser.create(router);
  }

  @Test
  public void absoluteLocalRef(VertxTestContext context) {
    URI sampleURI = buildBaseUri("ref_test", "sample.json");
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, buildBaseUri("ref_test", "test_1.json"));
    mainSchema.validateAsync("").onComplete(context.succeeding(o -> { // Trigger validation to start solve refs
      context.verify(() -> {
        MyAssertions.assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI), mainSchema.getScope(), parser).hasXIdEqualsTo("main");
        MyAssertions.assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI).append("definitions").append("sub1"), mainSchema.getScope(), parser).hasXIdEqualsTo("sub1");
      });
      context.completeNow();
    }));
  }

  @Test
  public void relativeLocalRef(VertxTestContext context) {
    URI sampleURI = URI.create("./sample.json");
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, Paths.get(".", "src", "test", "resources", "ref_test", "test_2.json").toUri());
    mainSchema.validateAsync("").onComplete(context.succeeding(o -> { // Trigger validation to start solve refs
      context.verify(() -> {
        MyAssertions.assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI), mainSchema.getScope(), parser).hasXIdEqualsTo("main");
        MyAssertions.assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI).append("definitions").append("sub1"), mainSchema.getScope(), parser).hasXIdEqualsTo("sub1");
      });
      context.completeNow();
    }));
  }

  @Test
  public void relativeLocalRefFromResources(VertxTestContext context) {
    URI sampleURI = vertx.resolveFile("ref_test/sample.json").toURI();
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, sampleURI.resolve("test_1.json"));
    mainSchema.validateAsync("").onComplete(context.succeeding(o -> { // Trigger validation to start solve refs
      context.verify(() -> {
        MyAssertions.assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI), mainSchema.getScope(), parser).hasXIdEqualsTo("main");
        MyAssertions.assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI).append("definitions").append("sub1"), mainSchema.getScope(), parser).hasXIdEqualsTo("sub1");
      });
      context.completeNow();
    }));
  }

  @Test
  public void jarURIRelativization(VertxTestContext context) {
    URI sampleURI = vertx.resolveFile("sample_in_jar.json").toURI();
    URI replaced1 = URIUtils.resolvePath(sampleURI, "empty_in_jar.json");
    URI replaced2 = URIUtils.resolvePath(sampleURI, "./empty_in_jar.json");
    context.verify(() -> {
      assertThat(vertx.resolveFile("empty_in_jar.json").toURI()).isEqualTo(replaced1);
      assertThat(vertx.resolveFile("empty_in_jar.json").toURI()).isEqualTo(replaced2);
    });
    context.completeNow();
  }

  @Test
  public void relativeLocalRefFromClassLoader(VertxTestContext context) {
    URI sampleURI = vertx.resolveFile("sample_in_jar.json").toURI();
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, URIUtils.resolvePath(sampleURI, "test_1.json"));
    mainSchema.validateAsync("").onComplete(context.succeeding(o -> { // Trigger validation to start solve refs
      context.verify(() -> {
        MyAssertions.assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI), mainSchema.getScope(), parser).hasXIdEqualsTo("main");
        MyAssertions.assertThat(router).canResolveSchema(JsonPointer.fromURI(sampleURI).append("definitions").append("sub1"), mainSchema.getScope(), parser).hasXIdEqualsTo("sub1");
      });
      context.completeNow();
    }));
  }

  @Test
  public void localRecursiveRef(VertxTestContext context) {
    Checkpoint check = context.checkpoint(2);

    URI sampleURI = buildBaseUri("ref_test", "person_recursive.json");
    JsonObject mainSchemaUnparsed = new JsonObject().put("$ref", sampleURI.toString());
    Schema mainSchema = parser.parse(mainSchemaUnparsed, buildBaseUri("ref_test", "test_1.json"));
    mainSchema.validateAsync(new JsonObject()
      .put("name", "Francesco")
      .put("surname", "Guardiani")
      .put("id_card", "ABC")
      .put("father", new JsonObject()
        .put("name", "Pietro")
        .put("surname", "Guardiani")
        .put("id_card", "XYZ")
      )
    ).onComplete(context.succeeding(o -> check.flag()));
    mainSchema.validateAsync(new JsonObject()
      .put("name", "Francesco")
      .put("surname", "Guardiani")
      .put("id_card", "ABC")
      .put("father", new JsonObject()
        .put("name", "Pietro")
        .put("surname", "Guardiani") // No id card!
      )
    ).onComplete(context.failing(o -> check.flag()));
  }

}
