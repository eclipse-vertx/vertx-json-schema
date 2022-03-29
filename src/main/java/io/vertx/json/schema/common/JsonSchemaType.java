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

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Predicate;

public enum JsonSchemaType {
  NULL(Objects::isNull),
  BOOLEAN(o -> o instanceof Boolean),
  OBJECT(JsonUtil::isObject),
  ARRAY(JsonUtil::isArray),
  NUMBER(o -> o instanceof Number),
  NUMBER_DECIMAL(o -> o instanceof Double || o instanceof Float),
  INTEGER(o -> {
    if (o instanceof Long || o instanceof Integer) {
      return true;
    }
    // Welcome to JSON world, given that type coercion is a thing
    // the spec mandates that we allow 1.0 to be treated as 1
    if (o instanceof Float) {
      float a = (float) o;
      return a % 1 == 0.0f;
    }
    if (o instanceof Double) {
      double a = (double) o;
      return a % 1 == 0.0;
    }
    if (o instanceof BigDecimal) {
      BigDecimal a = (BigDecimal) o;
      return a.remainder(BigDecimal.ONE).equals(BigDecimal.ZERO);
    }

    return false;
  }),
  STRING(o -> o instanceof String);

  private final Predicate<Object> checkInstancePredicate;

  JsonSchemaType(Predicate<Object> checkInstancePredicate) {
    this.checkInstancePredicate = checkInstancePredicate;
  }

  public boolean checkInstance(Object obj) {
    return checkInstancePredicate.test(obj);
  }
}
