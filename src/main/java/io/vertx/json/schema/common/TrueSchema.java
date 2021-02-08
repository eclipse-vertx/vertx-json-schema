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
package io.vertx.json.schema.common;

import io.vertx.core.Future;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.ValidationException;

public class TrueSchema implements SchemaInternal {

  private static class TrueSchemaHolder {
    static final TrueSchema INSTANCE = new TrueSchema(null);
  }

  public static TrueSchema getInstance() {
    return TrueSchemaHolder.INSTANCE;
  }

  MutableStateValidator parent;

  public TrueSchema(MutableStateValidator parent) {
    this.parent = parent;
  }

  @Override
  public boolean isSync() {
    return true;
  }

  @Override
  public ValidatorPriority getPriority() {
    return ValidatorPriority.MAX_PRIORITY;
  }

  @Override
  public void validateSync(Object in) throws ValidationException, NoSyncValidationException {
  }

  @Override
  public Future<Void> validateAsync(Object in) {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> validateAsync(ValidatorContext context, Object in) {
    return this.validateAsync(in);
  }

  @Override
  public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
    this.validateSync(in);
  }

  @Override
  public JsonPointer getScope() {
    return JsonPointer.create();
  }

  @Override
  public Boolean getJson() {
    return true;
  }

  @Override
  public Future<Object> getOrApplyDefaultAsync(Object input) {
    return Future.succeededFuture(input);
  }

  @Override
  public Object getOrApplyDefaultSync(Object input) {
    return input;
  }

}
