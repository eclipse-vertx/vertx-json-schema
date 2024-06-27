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

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.tests.impl.SchemaValidatorImpl;

import java.util.Collections;
import java.util.Objects;

import static io.vertx.json.schema.JsonFormatValidator.DEFAULT_VALIDATOR;

/**
 * A validator, validates some input object using a well known schema.
 *
 * @author Paulo Lopes
 */
@VertxGen
public interface Validator {


  /**
   * Creates a new validator with some initial schema and options.
   * <p>
   * When validation is to be reused, it is recommended to create a {@link SchemaRepository} instance and use the
   * {@link SchemaRepository#validator(String)}} method. The use of a {@link String} key allows avoiding re-parsing
   * and fast lookups.
   *
   * @param schema  the initial schema
   * @param options the validator options
   * @return a validator instance
   */
  static Validator create(JsonSchema schema, JsonSchemaOptions options) {
    return create(schema, options, DEFAULT_VALIDATOR);
  }

  /**
   * Creates a new validator with some initial schema, options and a custom JSON format validator.
   *
   * When validation is to be reused, it is recommended to create a {@link SchemaRepository} instance and use the
   * {@link SchemaRepository#validator(String)}} method. The use of a {@link String} key allows avoiding re-parsing
   * and fast lookups.
   *
   * @param schema the initial schema
   * @param options the validator options
   * @param jsonFormatValidator the custom JSON format validator
   * @return a validator instance
   */
  static Validator create(JsonSchema schema, JsonSchemaOptions options, JsonFormatValidator jsonFormatValidator) {
    Objects.requireNonNull(options.getBaseUri(), "'options.baseUri' cannot be null");
    return new SchemaValidatorImpl(schema, options, Collections.emptyMap(), true, jsonFormatValidator);
  }

  /**
   * Validate a given input against the initial schema.
   *
   * @param instance instance to validate
   * @return returns a output unit object as defined by the options
   * @throws SchemaException if the validation cannot complete, for example when a reference is missing.
   */
  OutputUnit validate(Object instance) throws SchemaException;
}
