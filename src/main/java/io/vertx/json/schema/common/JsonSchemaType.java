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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Objects;
import java.util.function.Predicate;

public enum JsonSchemaType {
  NULL(Objects::isNull),
  BOOLEAN(o -> o instanceof Boolean),
  OBJECT(o -> o instanceof JsonObject),
  ARRAY(o -> o instanceof JsonArray),
  NUMBER(o -> o instanceof Number),
  NUMBER_DECIMAL(o -> o instanceof Double || o instanceof Float),
  INTEGER(o -> o instanceof Long || o instanceof Integer),
  STRING(o -> o instanceof String);

  private final Predicate<Object> checkInstancePredicate;

  JsonSchemaType(Predicate<Object> checkInstancePredicate) {
    this.checkInstancePredicate = checkInstancePredicate;
  }

  public boolean checkInstance(Object obj) {
    return checkInstancePredicate.test(obj);
  }
}
