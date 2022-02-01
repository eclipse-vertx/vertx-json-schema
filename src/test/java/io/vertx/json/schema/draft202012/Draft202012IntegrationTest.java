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
package io.vertx.json.schema.draft202012;

import io.vertx.core.Vertx;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.BaseIntegrationTest;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.SchemaParserInternal;
import io.vertx.json.schema.common.SchemaRouterImpl;
import org.junit.jupiter.api.Disabled;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Francesco Guardiani @slinkydeveloper
 * @author Paulo Lopes @pml0p35
 */
public class Draft202012IntegrationTest extends BaseIntegrationTest {

  @Override
  public Map.Entry<SchemaParser, Schema> buildSchemaFunction(Vertx vertx, Object schema, String testFileName) {
    Draft202012SchemaParser parser = Draft202012SchemaParser.create(new SchemaRouterImpl(vertx, vertx.createHttpClient(), vertx.fileSystem(), new SchemaRouterOptions()));
    Schema s = parser.parse(schema, Paths.get(this.getTckPath() + "/" + testFileName + ".json").toAbsolutePath().toUri());
    return new AbstractMap.SimpleImmutableEntry<>(parser, s);
  }

  @Override
  public Stream<String> getTestFiles() {

    return Stream.of(
      "additionalProperties",
      "const",
      "dependentRequired",
      "exclusiveMinimum",
      //"items",
      "maxProperties",
      "minProperties",
      "pattern",
      "ref",
      //"unevaluatedProperties",
      "allOf",
      "contains",
      "dependentSchemas",
      "format",
      "maxContains",
      "minContains",
      "multipleOf",
      "patternProperties",
      //"refRemote",
      //"uniqueItems",
      "anchor",
      "content",
      //"dynamicRef",
      //"id",
      "maximum",
      "minimum",
      "not",
      "prefixItems",
      "required",
      "unknownKeyword",
      "anyOf",
      "default",
      "enum",
      "if-then-else",
      "maxItems",
      "minItems",
      "oneOf",
      "properties",
      "type",
      "vocabulary",
      "boolean_schema",
      "defs",
      "exclusiveMaximum",
      "infinite-loop-detection",
      "maxLength",
      "minLength",
      "propertyNames"
      //"unevaluatedItems"
    );
  }

  @Override
  public SchemaParserInternal getSchemaParser(Vertx vertx) {
    return Draft202012SchemaParser.create(new SchemaRouterImpl(vertx, vertx.createHttpClient(), vertx.fileSystem(), new SchemaRouterOptions()));
  }

  @Override
  public Path getTckPath() {
    return Paths.get("src", "test", "resources", "tck", "draft2020-12");
  }

  @Override
  public Path getRemotesPath() {
    return Paths.get("src", "test", "resources", "tck", "remotes");
  }

  @Override
  public boolean skipSyncCheck(Schema schema) {
    Object res = JsonPointer.create().append("$ref").queryJson(schema.getJson());
    if (res instanceof String) {
      return "json-schema.org".equals(URI.create((String) res).getHost());
    }
    return false;
  }
}
