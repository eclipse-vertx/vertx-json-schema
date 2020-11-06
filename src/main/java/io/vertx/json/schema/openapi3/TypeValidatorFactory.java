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
package io.vertx.json.schema.openapi3;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.SchemaException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.*;

public class TypeValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      String type = schema.getString("type");
      String format = schema.getString("format");
      if (type == null) throw new SchemaException(schema, "Null type keyword");
      return new TypeValidator(parseType(type, format, schema));
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for type/format/nullable keyword", e);
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
      default:
        throw new SchemaException(schema, "Unknown type: " + type);
    }
  }

  class TypeValidator extends BaseSyncValidator {

    final JsonSchemaType type;

    public TypeValidator(JsonSchemaType type) {
      this.type = type;
    }

    @Override
    public ValidatorPriority getPriority() {
      return ValidatorPriority.MAX_PRIORITY;
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException {
      if (in != null) {
        if (!type.checkInstance(in))
          throw ValidationException.createException("input don't match type " + type.name(), "type", in);
      }
    }
  }
}
