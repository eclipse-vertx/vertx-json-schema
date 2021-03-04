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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidatorFactory implements ValidatorFactory {

  @SuppressWarnings("unchecked")
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonArray allowedValues = (JsonArray) schema.getValue("enum");
      Set allowedValuesParsed = (Set) allowedValues
        .getList().stream()
        .map(o ->
          (o instanceof Map) ? new JsonObject((Map<String, Object>) o) :
            (o instanceof List) ? new JsonArray((List) o) :
              o
        ).collect(Collectors.toSet());
      return new EnumValidator(allowedValuesParsed);
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for enum keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null enum keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("enum");
  }

  public class EnumValidator extends BaseSyncValidator {
    private final Object[] allowedValues;

    public EnumValidator(Set allowedValues) {
      this.allowedValues = allowedValues.toArray();
    }

    @Override
    public ValidatorPriority getPriority() {
      return ValidatorPriority.MAX_PRIORITY;
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException {
      for (int i = 0; i < allowedValues.length; i++) {
        if (ComparisonUtils.equalsNumberSafe(allowedValues[i], in))
          return;
      }
      throw ValidationException.create("Input doesn't match one of allowed values of enum: " + Arrays.toString(allowedValues), "enum", in);
    }
  }

}
