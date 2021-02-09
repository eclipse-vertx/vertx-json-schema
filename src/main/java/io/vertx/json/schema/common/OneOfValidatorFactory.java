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
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.ValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.vertx.json.schema.ValidationException.createException;

public class OneOfValidatorFactory extends BaseCombinatorsValidatorFactory {

  @Override
  protected BaseCombinatorsValidator instantiate(MutableStateValidator parent) {
    return new OneOfValidator(parent);
  }

  @Override
  protected String getKeyword() {
    return "oneOf";
  }

  class OneOfValidator extends BaseCombinatorsValidator {

    public OneOfValidator(MutableStateValidator parent) {
      super(parent);
    }

    private boolean isValidSync(SchemaInternal schema, ValidatorContext context, Object in) {
      try {
        schema.validateSync(context, in);
        return true;
      } catch (ValidationException e) {
        return false;
      }
    }

    @Override
    public void validateSync(ValidatorContext context, Object in) throws ValidationException, NoSyncValidationException {
      this.checkSync();
      long validCount = Arrays.stream(schemas).map(s -> isValidSync(s, context, in)).filter(b -> b.equals(true)).count();
      if (validCount > 1) throw ValidationException.createException("More than one schema valid", "oneOf", in);
      else if (validCount == 0) throw ValidationException.createException("No schema matches", "oneOf", in);
    }

    @Override
    public Future<Void> validateAsync(ValidatorContext context, Object in) {
      if (isSync()) return validateSyncAsAsync(context, in);
      return FutureUtils
        .oneOf(Arrays.stream(schemas).map(s -> s.validateAsync(context, in)).collect(Collectors.toList()))
        .recover(err -> Future.failedFuture(createException("No schema matches", "oneOf", in, err)));
    }
  }

}