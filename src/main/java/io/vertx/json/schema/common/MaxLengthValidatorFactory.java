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
import io.vertx.json.schema.ValidationException;

public class MaxLengthValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number maximum = (Number) schema.getValue("maxLength");
      if (maximum.intValue() < 0)
        throw new SchemaException(schema, "maxLength must be >= 0");
      return new MaxLengthValidator(maximum.intValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for maxLength keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null maxLength keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("maxLength");
  }

  public class MaxLengthValidator extends BaseSyncValidator {
    private final int maximum;

    public MaxLengthValidator(int maximum) {
      this.maximum = maximum;
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException {
      if (in instanceof String) {
        if (((String) in).codePointCount(0, ((String) in).length()) > maximum) {
          throw ValidationException.createException("provided string should have size <= " + maximum, "maxLength", in);
        }
      }
    }
  }

}
