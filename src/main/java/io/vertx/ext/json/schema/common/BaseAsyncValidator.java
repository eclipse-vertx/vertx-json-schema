package io.vertx.ext.json.schema.common;

public abstract class BaseAsyncValidator implements AsyncValidator {

  @Override
  public boolean isSync() {
    return false;
  }

  @Override
  public ValidatorPriority getPriority() {
    return ValidatorPriority.MIN_PRIORITY;
  }

}
