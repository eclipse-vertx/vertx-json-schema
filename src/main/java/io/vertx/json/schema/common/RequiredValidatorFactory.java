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
import io.vertx.json.schema.ValidationException;

import java.util.HashSet;
import java.util.Set;

import static io.vertx.json.schema.ValidationException.createException;

public class RequiredValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator validator) {
    try {
      JsonArray keys = (JsonArray) schema.getValue("required");
      return new RequiredValidator(new HashSet(keys.getList()));
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for enum keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null enum keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("required");
  }

  public class RequiredValidator extends BaseSyncValidator {
    private final Set<String> requiredKeys;

    public RequiredValidator(Set<String> requiredKeys) {
      this.requiredKeys = requiredKeys;
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException {
      if (in instanceof JsonObject) {
        JsonObject obj = (JsonObject) in;
        for (String k : requiredKeys) {
          if (!obj.containsKey(k))
            throw createException("provided object should contain property " + k, "required", in);
        }
      }
    }
  }

}
