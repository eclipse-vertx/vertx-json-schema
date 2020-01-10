package io.vertx.ext.json.schema.common;

import io.vertx.ext.json.schema.ValidationException;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MinimumValidator extends BaseSyncValidator {
  private final double minimum;

  public MinimumValidator(double minimum) {
    this.minimum = minimum;
  }

  @Override
  public void validateSync(Object in) throws ValidationException {
    if (in instanceof Number) {
      if (((Number) in).doubleValue() < minimum) {
        throw createException("value should be >= " + minimum, "minimum", in);
      }
    }
  }
}
