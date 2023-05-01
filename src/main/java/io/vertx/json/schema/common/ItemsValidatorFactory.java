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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.ValidationException;

import java.util.ArrayList;
import java.util.List;

import static io.vertx.json.schema.common.JsonUtil.unwrap;

public class ItemsValidatorFactory extends BaseSingleSchemaValidatorFactory {

  @Override
  protected BaseSingleSchemaValidator instantiate(MutableStateValidator parent) {
    return new ItemsValidator(parent);
  }

  @Override
  protected String getKeyword() {
    return "items";
  }

  static class ItemsValidator extends BaseSingleSchemaValidator implements DefaultApplier {

    public ItemsValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      in = unwrap(in);
      if (in instanceof List<?>) {
        List<?> arr = (List<?>) in;
        for (int i = 0; i < arr.size(); i++) {
          context.markEvaluatedItem(i);
          schema.validateSync(context.lowerLevelContext(i), arr.get(i));
        }
      }
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      in = unwrap(in);
      if (in instanceof List<?>) {
        List<?> arr = (List<?>) in;
        List<Future<Void>> futs = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
          context.markEvaluatedItem(i);
          Future<Void> f = schema.validateAsync(context.lowerLevelContext(i), arr.get(i));
          if (f.isComplete()) {
            if (f.failed()) return Future.failedFuture(f.cause());
          } else {
            futs.add(f);
          }
        }
        if (futs.isEmpty())
          return Future.succeededFuture();
        else
          return CompositeFuture.all(futs).compose(cf -> Future.succeededFuture());
      } else return Future.succeededFuture();
    }

    @Override
    public Future<Void> applyDefaultValue(Object value) {
      value = unwrap(value);
      if (!(value instanceof List<?>)) {
        return Future.succeededFuture();
      }

      List<Future<?>> futures = new ArrayList<>();
      List<?> arr = (List<?>) value;
      for (Object valToDefault : arr) {
        if (schema.isSync()) {
          schema.getOrApplyDefaultSync(valToDefault);
        } else {
          futures.add(
            schema.getOrApplyDefaultAsync(valToDefault)
          );
        }
      }

      if (futures.isEmpty()) {
        return Future.succeededFuture();
      }

      return CompositeFuture.all(futures).mapEmpty();
    }
  }
}
