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

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.common.ValidatorFactory;
import io.vertx.json.schema.openapi3.OpenAPI3SchemaParser;

import java.util.function.Predicate;

/**
 * Parse a Json Schema. The parser can be extended to support custom keywords using {@link this#withValidatorFactory(ValidatorFactory)}
 * @deprecated users should migrate to the new validator
 */
@Deprecated
@VertxGen
public interface SchemaParser {

  /**
   * Build a schema from provided json assigning a random scope. This method registers the parsed schema (and relative subschemas) to the schema router
   *
   * @param jsonSchema JSON representing the schema
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException          If schema is invalid
   */
  Schema parse(JsonObject jsonSchema);

  /**
   * Build a schema from provided json. This method registers the parsed schema (and relative subschemas) to the schema router
   *
   * @param jsonSchema    JSON representing the schema
   * @param schemaPointer Scope of schema. Must be a JSONPointer with absolute URI
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException          If schema is invalid
   */
  Schema parse(JsonObject jsonSchema, JsonPointer schemaPointer);

  /**
   * Builds a true of false schema assigning a random scope
   *
   * @param jsonSchema JSON representing the schema
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException          If schema is invalid
   */
  Schema parse(Boolean jsonSchema);

  /**
   * Builds a true of false schema
   *
   * @param jsonSchema    JSON representing the schema
   * @param schemaPointer Scope of schema. Must be a JSONPointer with absolute URI
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException          If schema is invalid
   */
  Schema parse(Boolean jsonSchema, JsonPointer schemaPointer);

  /**
   * Build a schema from provided unparsed json assigning a random scope. This method registers the parsed schema (and relative subschemas) to the schema router
   *
   * @param unparsedJson Unparsed JSON representing the schema.
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException          If schema is invalid
   */
  Schema parseFromString(String unparsedJson);

  /**
   * Build a schema from provided unparsed json. This method registers the parsed schema (and relative subschemas) to the schema router
   *
   * @param unparsedJson  Unparsed JSON representing the schema.
   * @param schemaPointer Scope of schema. Must be a JSONPointer with absolute URI
   * @return the schema instance
   * @throws IllegalArgumentException If scope is relative
   * @throws SchemaException          If schema is invalid
   */
  Schema parseFromString(String unparsedJson, JsonPointer schemaPointer);

  /**
   * Get schema router registered to this schema parser
   *
   * @return
   */
  SchemaRouter getSchemaRouter();

  /**
   * Add a {@link ValidatorFactory} to this schema parser to support custom keywords
   *
   * @param factory new factory
   * @return a reference to this
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  @Fluent
  SchemaParser withValidatorFactory(ValidatorFactory factory);

  /**
   * Add a custom format validator
   *
   * @param formatName format name
   * @param predicate  predicate for the new format
   * @return a reference to this
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  @Fluent
  SchemaParser withStringFormatValidator(String formatName, Predicate<String> predicate);

  /**
   * Create a new {@link SchemaParser} for OpenAPI schemas
   *
   * @param router
   * @return
   */
  static SchemaParser createOpenAPI3SchemaParser(SchemaRouter router) {
    return OpenAPI3SchemaParser.create(router);
  }
}
