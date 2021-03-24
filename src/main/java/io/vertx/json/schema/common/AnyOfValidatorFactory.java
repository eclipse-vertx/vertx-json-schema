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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AnyOfValidatorFactory extends BaseCombinatorsValidatorFactory {

  @Override
  BaseCombinatorsValidator instantiate(MutableStateValidator parent) {
    return new AnyOfValidator(parent);
  }

  @Override
  String getKeyword() {
    return "anyOf";
  }

  static class AnyOfValidator extends BaseCombinatorsValidator {

    public AnyOfValidator(MutableStateValidator parent) {
      super(parent);
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      List<Throwable> res = null;
      for (SchemaInternal s : this.schemas) {
        try {
          s.validateSync(context, in);
          return;
        } catch (ValidationException e) {
          if (res == null) {
             res = new ArrayList<>();
          }
          res.add(e);
        }
      }
      throw ValidationException.create(
        "anyOf subschemas don't match",
        "anyOf",
        in,
        res
      );
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      return CompositeFuture
        .any(Arrays.stream(this.schemas).map(s -> s.validateAsync(context, in)).collect(Collectors.toList()))
        .compose(cf -> {
          if (cf.succeeded()) {
            return Future.succeededFuture();
          } else {
            return Future.failedFuture(
              ValidationException.create(
                "anyOf subschemas don't match",
                "anyOf",
                in,
                IntStream.range(0, cf.size()).mapToObj(cf::cause).filter(Objects::nonNull).collect(Collectors.toList()))
            );
          }
        });
    }

  }

}
