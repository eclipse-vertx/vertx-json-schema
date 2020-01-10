package io.vertx.ext.json.schema.common;

public abstract class BaseSyncValidator implements SyncValidator {

  @Override
  public boolean isSync() {
    return true;
  }

  @Override
  public ValidatorPriority getPriority() {
    return ValidatorPriority.MIN_PRIORITY;
  }

}
