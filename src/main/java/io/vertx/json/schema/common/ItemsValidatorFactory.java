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
import io.vertx.core.json.JsonArray;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.ValidationException;

import java.util.ArrayList;
import java.util.List;

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
      if (in instanceof JsonArray) {
        in = ((JsonArray) in).getList();
      }
      if (in instanceof List) {
        List<?> arr = (List<?>) in;
        for (int i = 0; i < arr.size(); i++) {
          context.markEvaluatedItem(i);
          schema.validateSync(context.lowerLevelContext(), arr.get(i));
        }
      }
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      if (in instanceof JsonArray) {
        in = ((JsonArray) in).getList();
      }
      if (in instanceof List) {
        List<?> arr = (List<?>) in;
        List<Future> futs = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
          context.markEvaluatedItem(i);
          Future<Void> f = schema.validateAsync(context.lowerLevelContext(), arr.get(i));
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
    public Future<Void> applyDefaultValue(Object in) {
      if (in instanceof JsonArray) {
        in = ((JsonArray) in).getList();
      }

      if (!(in instanceof List)) {
        return Future.succeededFuture();
      }

      List<Future> futures = new ArrayList<>();
      List<?> arr = (List<?>) in;
      for (Object o : arr) {
        if (schema.isSync()) {
          schema.getOrApplyDefaultSync(o);
        } else {
          futures.add(
            schema.getOrApplyDefaultAsync(o)
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
