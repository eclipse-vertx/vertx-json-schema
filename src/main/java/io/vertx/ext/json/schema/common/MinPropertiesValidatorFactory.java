package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MinPropertiesValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number minimum = (Number) schema.getValue("minProperties");
      if (minimum.intValue() < 0)
        throw new SchemaException(schema, "minProperties must be >= 0");
      return new MinPropertiesValidator(minimum.intValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for minProperties keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null minProperties keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("minProperties");
  }

  public class MinPropertiesValidator extends BaseSyncValidator {
    private final int minimum;

    public MinPropertiesValidator(int minimum) {
      this.minimum = minimum;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (in instanceof JsonObject) {
        if (((JsonObject) in).size() < minimum) {
          throw createException("provided object should have size >= " + minimum, "minProperties", in);
        }
      }
    }
  }

}
