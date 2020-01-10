package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MaxItemsValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number maximum = (Number) schema.getValue("maxItems");
      if (maximum.intValue() < 0)
        throw new SchemaException(schema, "maxItems must be >= 0");
      return new MaxItemsValidator(maximum.intValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for maxItems keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null maxItems keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("maxItems");
  }

  public class MaxItemsValidator extends BaseSyncValidator {
    private final int maximum;

    public MaxItemsValidator(int maximum) {
      this.maximum = maximum;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (in instanceof JsonArray) {
        if (((JsonArray) in).size() > maximum) {
          throw createException("provided array should have size <= " + maximum, "maxItems", in);
        }
      }
    }
  }

}
