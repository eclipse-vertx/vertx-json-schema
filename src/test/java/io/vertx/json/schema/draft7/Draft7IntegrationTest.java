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
package io.vertx.json.schema.draft7;

import io.vertx.core.Vertx;
import io.vertx.json.schema.BaseIntegrationTest;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.SchemaParserInternal;
import io.vertx.json.schema.common.SchemaRouterImpl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
public class Draft7IntegrationTest extends BaseIntegrationTest {

  @Override
  public Stream<String> getTestFiles() {
    return Stream.of(
      "additionalItems",
      "additionalProperties",
      "allOf",
      "anyOf",
      "boolean_schema",
      "const",
      "contains",
      "definitions",
      "dependencies",
      "enum",
      "exclusiveMaximum",
      "exclusiveMinimum",
      "if-then-else",
      "format",
      "items",
      "maximum",
      "maxItems",
      "maxLength",
      "maxProperties",
      "minimum",
      "minItems",
      "minLength",
      "minProperties",
      "multipleOf",
      "not",
      "oneOf",
      "pattern",
      "patternProperties",
      "properties",
      "propertyNames",
      "ref",
      "refRemote",
      "required",
      "type",
      "uniqueItems"
    );
  }

  @Override
  public SchemaParserInternal getSchemaParser(Vertx vertx) {
    return Draft7SchemaParser.create(new SchemaRouterImpl(vertx.createHttpClient(), vertx.fileSystem(), new SchemaRouterOptions()));
  }

  @Override
  public Path getTckPath() {
    return Paths.get("src", "test", "resources", "tck", "draft7");
  }

  @Override
  public Path getRemotesPath() {
    return Paths.get("src", "test", "resources", "tck", "draft7", "remotes");
  }
}
