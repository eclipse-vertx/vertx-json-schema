package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MaxLengthValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number maximum = (Number) schema.getValue("maxLength");
      if (maximum.intValue() < 0)
        throw new SchemaException(schema, "maxLength must be >= 0");
      return new MaxLengthValidator(maximum.intValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for maxLength keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null maxLength keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("maxLength");
  }

  public class MaxLengthValidator extends BaseSyncValidator {
    private final int maximum;

    public MaxLengthValidator(int maximum) {
      this.maximum = maximum;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (in instanceof String) {
        if (((String) in).codePointCount(0, ((String) in).length()) > maximum) {
          throw createException("provided string should have size <= " + maximum, "maxLength", in);
        }
      }
    }
  }

}
