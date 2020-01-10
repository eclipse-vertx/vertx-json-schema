package io.vertx.ext.json.schema.common;

import io.vertx.core.Future;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;

public abstract class BaseMutableStateValidator implements MutableStateValidator {

  boolean isSync;
  final private MutableStateValidator parent;

  public BaseMutableStateValidator(MutableStateValidator parent) {
    this.parent = parent;
    this.isSync = false;
  }

  public abstract boolean calculateIsSync();

  protected Future<Void> validateSyncAsAsync(Object in) {
    try {
      validateSync(in);
      triggerUpdateIsSync();
      return Future.succeededFuture();
    } catch (ValidationException e) {
      return Future.failedFuture(e);
    }
  }

  protected void initializeIsSync() {
    isSync = calculateIsSync();
  }

  @Override
  public void triggerUpdateIsSync() {
    boolean calculated = calculateIsSync();
    boolean previous = isSync;
    isSync = calculated;
    if (calculated != previous && getParent() != null)
      getParent().triggerUpdateIsSync();
  }

  @Override
  public MutableStateValidator getParent() { return parent; }

  protected void checkSync() throws ValidationException, NoSyncValidationException {
    if (!isSync) throw new NoSyncValidationException("Trying to execute validateSync() for a Validator in asynchronous state", this);
  }

  @Override
  public boolean isSync() {
    return isSync;
  }

  @Override
  public ValidatorPriority getPriority() {
    return ValidatorPriority.MIN_PRIORITY;
  }
}
