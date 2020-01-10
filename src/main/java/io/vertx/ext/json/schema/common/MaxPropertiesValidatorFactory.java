package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MaxPropertiesValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number maximum = (Number) schema.getValue("maxProperties");
      if (maximum.intValue() < 0)
        throw new SchemaException(schema, "maxProperties must be >= 0");
      return new MaxPropertiesValidator(maximum.intValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for maxProperties keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null maxProperties keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("maxProperties");
  }

  public class MaxPropertiesValidator extends BaseSyncValidator {
    private final int maximum;

    public MaxPropertiesValidator(int maximum) {
      this.maximum = maximum;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (in instanceof JsonObject) {
        if (((JsonObject) in).size() > maximum) {
          throw createException("provided object should have size <= " + maximum, "maxProperties", in);
        }
      }
    }
  }

}
