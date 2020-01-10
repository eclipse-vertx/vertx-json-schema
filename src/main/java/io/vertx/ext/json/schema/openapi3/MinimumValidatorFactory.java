package io.vertx.ext.json.schema.openapi3;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;
import io.vertx.ext.json.schema.common.*;

public class MinimumValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number maximum = (Number) schema.getValue("minimum");
      if (schema.containsKey("exclusiveMinimum") && schema.getBoolean("exclusiveMinimum"))
        return new ExclusiveMinimumValidator(maximum.doubleValue());
      return new MinimumValidator(maximum.doubleValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for minimum or exclusiveMinimum keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null minimum or exclusiveMinimum keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("minimum");
  }

}
