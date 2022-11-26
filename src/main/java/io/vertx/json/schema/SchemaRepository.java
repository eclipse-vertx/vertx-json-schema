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
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.impl.SchemaRepositoryImpl;

/**
 * A repository is a holder of dereferenced schemas, it can be used to create validator instances for a specific schema.
 * <p>
 * This is to be used when multiple schema objects compose the global schema to be used for validation.
 *
 * @author Paulo Lopes
 */
@VertxGen
public interface SchemaRepository {

  /**
   * Create a repository with some initial configuration.
   *
   * @param options the initial configuration
   * @return a repository
   */
  static SchemaRepository create(JsonSchemaOptions options) {
    return new SchemaRepositoryImpl(options);
  }

  /**
   * Dereferences a schema to the repository.
   *
   * @param schema a new schema to list
   * @return a repository
   * @throws SchemaException when a schema is already present for the same id
   */
  @Fluent
  SchemaRepository dereference(JsonSchema schema) throws SchemaException;

  /**
   * Dereferences a schema to the repository.
   *
   * @param uri    the source of the schema used for de-referencing, optionally relative to
   *               {@link JsonSchemaOptions#getBaseUri()}.
   * @param schema a new schema to list
   * @return a repository
   * @throws SchemaException when a schema is already present for the same id
   */
  @Fluent
  SchemaRepository dereference(String uri, JsonSchema schema) throws SchemaException;

  /**
   * Preloads the repository with the meta schemas for the related @link {@link Draft} version. The related draft version
   * is determined from the {@link JsonSchemaOptions}, in case that no draft is set in the options an
   * {@link IllegalStateException} is thrown.
   *
   * @param fs The Vert.x file system to load the related schema meta files from classpath
   * @return a repository
   */
  @Fluent
  SchemaRepository preloadMetaSchema(FileSystem fs);

  /**
   * Preloads the repository with the meta schemas for the related draft version.
   *
   * @param fs    The Vert.x file system to load the related schema meta files from classpath
   * @param draft The draft version of the meta files to load
   * @return a repository
   */
  @Fluent
  SchemaRepository preloadMetaSchema(FileSystem fs, Draft draft);

  /**
   * A new validator instance using this repository options.
   *
   * @param schema the start validation schema
   * @return the validator
   */
  Validator validator(JsonSchema schema);

  /**
   * A new validator instance using this repository options.
   *
   * @param ref the start validation reference in JSON pointer format
   * @return the validator
   */
  Validator validator(String ref);

  /**
   * A new validator instance overriding this repository options.
   *
   * @param schema  the start validation schema
   * @param options the options to be using on the validator instance
   * @return the validator
   */
  Validator validator(JsonSchema schema, JsonSchemaOptions options);

  /**
   * A new validator instance overriding this repository options.
   *
   * @param ref     the start validation reference in JSON pointer format
   * @param options the options to be using on the validator instance
   * @return the validator
   */
  Validator validator(String ref, JsonSchemaOptions options);

  /**
   * Tries to resolve all internal and repository local references. External references are not resolved.
   * <p>
   * The result is an object where all references have been resolved. Resolution of references is shallow. This
   * should normally not be a problem for this use case.
   *
   * @return a new {@link JsonObject} representing the schema with {@code $ref}s replaced by their value.
   * @throws SchemaException when the resolution is impossible. One of such cases is circular referencing.
   */
  JsonObject resolve(JsonSchema schema);

  /**
   * Tries to resolve all internal and repository local references. External references are not resolved.
   * <p>
   * The result is an object where all references have been resolved. Resolution of references is shallow. This
   * should normally not be a problem for this use case.
   *
   * @param ref the start resolution reference in JSON pointer format
   * @return a new {@link JsonObject} representing the schema with {@code $ref}s replaced by their value.
   * @throws SchemaException when the resolution is impossible. One of such cases is circular referencing.
   */
  JsonObject resolve(String ref);

  /**
   * Look up a schema using a JSON pointer notation
   *
   * @param pointer the JSON pointer
   * @return the schema
   */
  JsonSchema find(String pointer);
}
