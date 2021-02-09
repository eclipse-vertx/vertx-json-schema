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

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaException;

import java.util.Map;

public class DefinitionsValidatorFactory implements ValidatorFactory {

  private final String definitionsKey;

  public DefinitionsValidatorFactory(String definitionsKey) {
    this.definitionsKey = definitionsKey;
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonObject definitions = schema.getJsonObject(this.definitionsKey);
      JsonPointer basePointer = scope.append(this.definitionsKey);
      definitions.forEach(e -> {
        parser.parse((e.getValue() instanceof Map) ? new JsonObject((Map<String, Object>) e.getValue()) : e.getValue(), basePointer.copy().append(e.getKey()), null);
      });
      return null;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for " + this.definitionsKey + " keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null " + this.definitionsKey + " keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey(this.definitionsKey);
  }

}
