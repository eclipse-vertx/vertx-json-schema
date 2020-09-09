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

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.SchemaException;

public abstract class BaseSingleSchemaValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Object itemsSchema = schema.getValue(getKeyword());
      BaseSingleSchemaValidator validator = instantiate(parent);
      validator.setSchema(parser.parse(itemsSchema, scope.append(getKeyword()), validator));
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

  protected abstract BaseSingleSchemaValidator instantiate(MutableStateValidator parent);

  protected abstract String getKeyword();
}
