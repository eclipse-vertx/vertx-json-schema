package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MinItemsValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number minimum = (Number) schema.getValue("minItems");
      if (minimum.intValue() < 0)
        throw new SchemaException(schema, "minItems must be >= 0");
      return new MinItemsValidator(minimum.intValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for minItems keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null minItems keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("minItems");
  }

  public class MinItemsValidator extends BaseSyncValidator {
    private final int minimum;

    public MinItemsValidator(int minimum) {
      this.minimum = minimum;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (in instanceof JsonArray) {
        if (((JsonArray) in).size() < minimum) {
          throw createException("provided array should have size >= " + minimum, "minItems", in);
        }
      }
    }
  }

}
