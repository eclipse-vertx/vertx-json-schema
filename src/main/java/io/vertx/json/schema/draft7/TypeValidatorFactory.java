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
package io.vertx.json.schema.draft7;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      List<JsonSchemaType> allowedTypes = new ArrayList<>();
      Object type = schema.getValue("type");
      String format = schema.getString("format");
      if (type instanceof String) allowedTypes.add(parseType((String) type, format, schema));
      else {
        JsonArray types = (JsonArray) type;
        for (Object t : types) allowedTypes.add(parseType((String) t, format, schema));
      }
      boolean allowNull = allowedTypes.contains(JsonSchemaType.NULL);
      if (allowNull) allowedTypes.remove(JsonSchemaType.NULL);
      return new TypeValidator(allowedTypes.toArray(new JsonSchemaType[0]), allowNull);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null type keyword", e);
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for type or format keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("type");
  }

  private static JsonSchemaType parseType(String type, String format, JsonObject schema) {
    switch (type) {
      case "integer":
        return JsonSchemaType.INTEGER;
      case "number":
        return (format != null && (format.equals("double") || format.equals("float"))) ? JsonSchemaType.NUMBER_DECIMAL : JsonSchemaType.NUMBER;
      case "boolean":
        return JsonSchemaType.BOOLEAN;
      case "string":
        return JsonSchemaType.STRING;
      case "object":
        return JsonSchemaType.OBJECT;
      case "array":
        return JsonSchemaType.ARRAY;
      case "null":
        return JsonSchemaType.NULL;
      default:
        throw new SchemaException(schema, "Unknown type: " + type);
    }
  }

  static class TypeValidator extends BaseSyncValidator {

    final JsonSchemaType[] types;
    final boolean nullIsValid;

    public TypeValidator(JsonSchemaType[] types, boolean nullIsValid) {
      this.types = types;
      this.nullIsValid = nullIsValid;
    }

    @Override
    public ValidatorPriority getPriority() {
      return ValidatorPriority.MAX_PRIORITY;
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException {
      if (in != null) {
        for (JsonSchemaType type : types) if (type.checkInstance(in)) return;
        throw ValidationException.create("input don't match any of types " + Arrays.deepToString(types), "type", in);
      } else if (!nullIsValid)
        throw ValidationException.create("input don't match any of types " + Arrays.deepToString(types), "type", null);
    }
  }
}
