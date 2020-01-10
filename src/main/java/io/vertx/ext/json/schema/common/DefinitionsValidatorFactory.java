package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import java.util.Map;

public class DefinitionsValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonObject definitions = schema.getJsonObject("definitions");
      JsonPointer basePointer = scope.append("definitions");
      definitions.forEach(e -> {
        parser.parse((e.getValue() instanceof Map) ? new JsonObject((Map<String, Object>) e.getValue()) : e.getValue(), basePointer.copy().append(e.getKey()), null);
      });
      return null;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for definitions keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null definitions keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("definitions");
  }

}
