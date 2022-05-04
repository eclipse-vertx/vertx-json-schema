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
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.impl.BooleanSchema;

import java.util.Set;

/**
 * A Json-Schema holder.
 *
 * There are 2 kinds of Json-Schema's:
 *
 * <ul>
 *   <li>JSON Object based</li>
 *   <li>Boolean based</li>
 * </ul>
 *
 * This is a common interface to handle all kids of schemas.
 *
 * @author Paulo Lopes
 */
@VertxGen
public interface JsonSchema {

  /**
   * Factory method to create a {@link JsonSchema} from a {@link JsonObject}.
   * @param json a JSON Object.
   * @return a wrapper for the input object.
   */
  static JsonSchema of(JsonObject json) {
    return new io.vertx.json.schema.impl.JsonSchema(json);
  }

  /**
   * Factory method to create a {@link JsonSchema} from a {@link JsonObject}.
   * @param id will force the given id as the schema $id.
   * @param json a JSON Object.
   * @return a wrapper for the input object.
   */
  static JsonSchema of(String id, JsonObject json) {
    return new io.vertx.json.schema.impl.JsonSchema(
      json.copy()
        .put("id", id));
  }

  /**
   * Factory method to create a {@link JsonSchema} from a {@link Boolean}.
   * @param bool a boolean.
   * @return a wrapper for the input object.
   */
  static JsonSchema of(boolean bool) {
    return bool ?
      BooleanSchema.TRUE :
      BooleanSchema.FALSE;
  }

  /**
   * Annotates the schema. An annotation is a extra key-value added to the schema that are not relevant for
   * validation but can be used to store pre-computed state.
   *
   * @param key a key
   * @param value a value
   */
  void annotate(String key, String value);

  /**
   * Get a type casted value by key.
   *
   * @param key a key
   * @return the value or {@code null}
   */
  <R> R get(String key);

  /**
   * Get a type casted value by key. If the key is missing, then the fallback value is returned.
   *
   * @param key a key
   * @param fallback fallback when key is not present
   * @return the value or {@code null}
   */
  <R> R get(String key, R fallback);

  /**
   * Checks if the given key is present in the schema object.
   *
   * @param key a key
   * @return {@code true} if present
   */
  boolean containsKey(String key);

  /**
   * Returns the field names on the underlying object.
   *
   * @return field names
   */
  Set<String> fieldNames();
}