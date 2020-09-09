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

import io.vertx.ext.json.schema.ValidationException;

import static io.vertx.ext.json.schema.ValidationException.createException;

public class MaximumValidator extends BaseSyncValidator {
  private final double maximum;

  public MaximumValidator(double maximum) {
    this.maximum = maximum;
  }

  @Override
  public void validateSync(ValidatorContext context, Object in) throws ValidationException {
    if (in instanceof Number) {
      if (((Number) in).doubleValue() > maximum) {
        throw createException("value should be <= " + maximum, "maximum", in);
      }
    }
  }
}
