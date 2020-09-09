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
import io.vertx.ext.json.schema.ValidationException;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MultipleOfValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number multipleOf = (Number) schema.getValue("multipleOf");
      return new MultipleOfValidator(multipleOf.doubleValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for multipleOf keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null multipleOf keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("multipleOf");
  }

  class MultipleOfValidator extends BaseSyncValidator {
    private final double multipleOf;

    public MultipleOfValidator(double multipleOf) {
      this.multipleOf = multipleOf;
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException {
      if (in instanceof Number) {
        if (((Number) in).doubleValue() % multipleOf != 0) {
          throw createException("provided number should be multiple of " + multipleOf, "multipleOf", in);
        }
      }
    }
  }

}
