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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaException;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCombinatorsValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonArray allOfSchemas = schema.getJsonArray(getKeyword());
      if (allOfSchemas.size() == 0)
        throw new SchemaException(schema, getKeyword() + " must have at least one element");
      JsonPointer basePointer = scope.append(getKeyword());
      List<SchemaInternal> parsedSchemas = new ArrayList<>();

      BaseCombinatorsValidator validator = instantiate(parent);
      for (int i = 0; i < allOfSchemas.size(); i++) {
        parsedSchemas.add(parser.parse(allOfSchemas.getValue(i), basePointer.copy().append(Integer.toString(i)), validator));
      }
      validator.setSchemas(parsedSchemas);
      return validator;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for " + getKeyword() + " keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null " + getKeyword() + " keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey(getKeyword());
  }

  abstract BaseCombinatorsValidator instantiate(MutableStateValidator parent);

  abstract String getKeyword();
}
