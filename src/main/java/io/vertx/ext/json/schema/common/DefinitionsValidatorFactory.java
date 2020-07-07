package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.SchemaException;

import java.util.Map;

public class DefinitionsValidatorFactory implements ValidatorFactory {

  private final String definitionsKey;

  public DefinitionsValidatorFactory(String definitionsKey) {
    this.definitionsKey = definitionsKey;
  }

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonObject definitions = schema.getJsonObject(this.definitionsKey);
      JsonPointer basePointer = scope.append(this.definitionsKey);
      definitions.forEach(e -> {
        parser.parse((e.getValue() instanceof Map) ? new JsonObject((Map<String, Object>) e.getValue()) : e.getValue(), basePointer.copy().append(e.getKey()), null);
      });
      return null;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for " + this.definitionsKey + " keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null " + this.definitionsKey + " keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey(this.definitionsKey);
  }

}
