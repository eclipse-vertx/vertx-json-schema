package io.vertx.ext.json.schema.common;

import io.vertx.ext.json.schema.ValidationException;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MaximumValidator extends BaseSyncValidator {
  private final double maximum;

  public MaximumValidator(double maximum) {
    this.maximum = maximum;
  }

  @Override
  public void validateSync(Object in) throws ValidationException {
    if (in instanceof Number) {
      if (((Number) in).doubleValue() > maximum) {
        throw createException("value should be <= " + maximum, "maximum", in);
      }
    }
  }
}
