/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
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

  protected Future<Void> validateSyncAsAsync(ValidatorContext context, Object in) {
    try {
      validateSync(context, in);
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
  public MutableStateValidator getParent() {
    return parent;
  }

  protected void checkSync() throws ValidationException, NoSyncValidationException {
    if (!isSync)
      throw new NoSyncValidationException("Trying to execute validateSync() for a Validator in asynchronous state", this);
  }

  @Override
  public boolean isSync() {
    return isSync;
  }

  @Override
  public ValidatorPriority getPriority() {
    return ValidatorPriority.NORMAL_PRIORITY;
  }
}
