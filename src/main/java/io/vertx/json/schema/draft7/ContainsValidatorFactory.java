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
package io.vertx.json.schema.draft7;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.ValidationException;
import io.vertx.json.schema.common.BaseSingleSchemaValidator;
import io.vertx.json.schema.common.BaseSingleSchemaValidatorFactory;
import io.vertx.json.schema.common.MutableStateValidator;
import io.vertx.json.schema.common.ValidatorContext;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ContainsValidatorFactory extends BaseSingleSchemaValidatorFactory {

  @Override
  protected BaseSingleSchemaValidator instantiate(MutableStateValidator parent) {
    return new ContainsValidator(parent);
  }

  @Override
  protected String getKeyword() {
    return "contains";
  }

  class ContainsValidator extends BaseSingleSchemaValidator {

    public ContainsValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      if (in instanceof JsonArray) {
        JsonArray arr = (JsonArray) in;
        if (arr.isEmpty())
          return Future.failedFuture(ValidationException.createException("provided array should not be empty", "contains", in));
        else
          return CompositeFuture.any(
            arr
              .stream()
              .map(i -> schema.validateAsync(context.lowerLevelContext(), in))
              .collect(Collectors.toList())
          ).compose(
            cf -> {
              IntStream.rangeClosed(0, cf.size())
                .forEach(i -> {
                  if (cf.succeeded(i)) {
                    context.markEvaluatedItem(i);
                  }
                });
              return Future.succeededFuture();
            },
            err -> Future.failedFuture(ValidationException.createException("provided array doesn't contain an element matching the contains schema", "contains", in, err))
          );
      } else return Future.succeededFuture();
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      ValidationException t = null;
      if (in instanceof JsonArray) {
        JsonArray arr = (JsonArray) in;
        for (int i = 0; i < arr.size(); i++) {
          try {
            schema.validateSync(context.lowerLevelContext(), arr.getValue(i));
            context.markEvaluatedItem(i);
            return;
          } catch (ValidationException e) {
            t = e;
          }
        }
        throw ValidationException.createException("provided array doesn't contain an element matching the contains schema", "contains", in, t);
      }
    }

  }

}
