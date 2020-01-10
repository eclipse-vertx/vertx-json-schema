package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

public abstract class BaseSingleSchemaValidatorFactory implements ValidatorFactory {
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Object itemsSchema = schema.getValue(getKeyword());
      BaseSingleSchemaValidator validator = instantiate(parent);
      validator.setSchema(parser.parse(itemsSchema, scope.append(getKeyword()), validator));
      return validator;
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for " + getKeyword() + " keyword",  e);
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
