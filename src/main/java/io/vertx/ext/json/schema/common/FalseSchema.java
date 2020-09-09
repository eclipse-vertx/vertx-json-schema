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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.json.schema.NoSyncValidationException;
import io.vertx.ext.json.schema.ValidationException;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class FalseSchema implements SchemaInternal {

  private static class FalseSchemaHolder {
    static final FalseSchema INSTANCE = new FalseSchema(null);
  }

  public static FalseSchema getInstance() {
    return FalseSchemaHolder.INSTANCE;
  }

  MutableStateValidator parent;

  public FalseSchema(MutableStateValidator parent) {
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
    throw createException("False schema always fail validation", null, in);
  }

  @Override
  public Future<Void> validateAsync(Object in) {
    return Future.failedFuture(createException("False schema always fail validation", null, in));
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
    return false;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }

  @Override
  public boolean hasDefaultValue() {
    return false;
  }

  @Override
  public void applyDefaultValues(JsonArray array) throws NoSyncValidationException {
  }

  @Override
  public void applyDefaultValues(JsonObject object) throws NoSyncValidationException {
  }

}
