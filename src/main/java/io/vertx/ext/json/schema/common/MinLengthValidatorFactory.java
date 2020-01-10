package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MinLengthValidatorFactory implements ValidatorFactory {

  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
    try {
      Number minimum = (Number) schema.getValue("minLength");
      if (minimum.intValue() < 0)
        throw new SchemaException(schema, "minLength must be >= 0");
      return new MinLengthValidator(minimum.intValue());
    } catch (ClassCastException e) {
      throw new SchemaException(schema, "Wrong type for minLength keyword", e);
    } catch (NullPointerException e) {
      throw new SchemaException(schema, "Null minLength keyword", e);
    }
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("minLength");
  }

  public class MinLengthValidator extends BaseSyncValidator {
    private final int minimum;

    public MinLengthValidator(int minimum) {
      this.minimum = minimum;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (in instanceof String) {
        if (((String) in).codePointCount(0, ((String) in).length()) < minimum) {
          throw createException("provided string should have size >= " + minimum, "minLength", in);
        }
      }
    }
  }

}
