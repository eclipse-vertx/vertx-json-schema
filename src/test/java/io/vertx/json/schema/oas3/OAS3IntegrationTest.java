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
package io.vertx.json.schema.oas3;

import io.vertx.core.Vertx;
import io.vertx.json.schema.BaseIntegrationTest;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.SchemaParserInternal;
import io.vertx.json.schema.common.SchemaRouterImpl;
import io.vertx.json.schema.openapi3.OpenAPI3SchemaParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * @author Francesco Guardiani @slinkydeveloper
 */
public class OAS3IntegrationTest extends BaseIntegrationTest {

  @Override
  public Stream<String> getTestFiles() {
    return Stream.of(
      "additionalProperties",
      "allOf",
      "anyOf",
      "discriminator",
      "enum",
      "exclusiveMaximum",
      "exclusiveMinimum",
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
      "nullable",
      "oneOf",
      "pattern",
      "properties",
      "ref",
      "refRemote",
      "required",
      "type",
      "uniqueItems"
    );
  }

  @Override
  public SchemaParserInternal getSchemaParser(Vertx vertx) {
    return OpenAPI3SchemaParser.create(new SchemaRouterImpl(vertx, vertx.createHttpClient(), vertx.fileSystem(), new SchemaRouterOptions()));
  }

  @Override
  public Path getTckPath() {
    return Paths.get("src", "test", "resources", "tck", "openapi3");
  }

  @Override
  public Path getRemotesPath() {
    return Paths.get("src", "test", "resources", "tck", "openapi3", "remotes");
  }
}
