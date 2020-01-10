package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.*;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class ConstValidatorFactory implements ValidatorFactory {

  @SuppressWarnings("unchecked")
  @Override
  public Validator createValidator(JsonObject schema, JsonPointer scope, SchemaParserInternal parser, MutableStateValidator parent) {
      Object allowedValue = schema.getValue("const");
      return new ConstValidator(allowedValue);
  }

  @Override
  public boolean canConsumeSchema(JsonObject schema) {
    return schema.containsKey("const");
  }

  public class ConstValidator extends BaseSyncValidator {

    private final Object allowedValue;

    public ConstValidator(Object allowedValue) {
      this.allowedValue = allowedValue;
    }

    @Override
    public ValidatorPriority getPriority() {
      return ValidatorPriority.MAX_PRIORITY;
    }

    @Override
    public void validateSync(Object in) throws ValidationException {
      if (!ComparisonUtils.equalsNumberSafe(allowedValue, in))
        throw createException("Input doesn't match const: " + allowedValue, "const", in);
    }
  }

}
