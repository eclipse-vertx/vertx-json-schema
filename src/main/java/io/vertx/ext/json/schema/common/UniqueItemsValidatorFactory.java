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
package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.SchemaException;
import io.vertx.ext.json.schema.ValidationException;

import java.util.HashSet;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class UniqueItemsValidatorFactory implements ValidatorFactory {

  private final static BaseSyncValidator UNIQUE_VALIDATOR = new BaseSyncValidator() {
    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      if (in instanceof JsonArray) {
        JsonArray arr = (JsonArray) in;
        if (new HashSet(arr.getList()).size() != arr.size())
          throw createException("array elements must be unique", "uniqueItems", in);
      }
    }
  };

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator validator) {
    try {
      Boolean unique = (Boolean) schema.getValue("uniqueItems");
      if (unique) return UNIQUE_VALIDATOR;
      else return null;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for uniqueItems keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null uniqueItems keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("uniqueItems");
  }

}
