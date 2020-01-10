package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCombinatorsValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      JsonArray allOfSchemas = schema.getJsonArray(getKeyword());
      if (allOfSchemas.size() == 0)
        throw new SchemaException(schema, getKeyword() + " must have at least one element");
      JsonPointer basePointer = scope.append(getKeyword());
      List<Schema> parsedSchemas = new ArrayList<>();

      BaseCombinatorsValidator validator = instantiate(parent);
      for (int i = 0; i < allOfSchemas.size(); i++) {
        parsedSchemas.add(parser.parse(allOfSchemas.getValue(i), basePointer.copy().append(Integer.toString(i)), validator));
      }
      validator.setSchemas(parsedSchemas);
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

  abstract BaseCombinatorsValidator instantiate(MutableStateValidator parent);
  abstract String getKeyword();
}
