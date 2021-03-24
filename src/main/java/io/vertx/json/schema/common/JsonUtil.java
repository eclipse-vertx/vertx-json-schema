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

import java.util.List;
import java.util.Map;

public final class JsonUtil {
  private JsonUtil() {}

  public static boolean isObject(Object o) {
    return o instanceof JsonObject || o instanceof Map;
  }

  public static boolean isArray(Object o) {
    return o instanceof JsonArray || o instanceof List;
  }

  public static Object unwrap(Object o) {
    if (o instanceof JsonObject) {
      return ((JsonObject) o).getMap();
    }
    if (o instanceof JsonArray) {
      return ((JsonArray) o).getList();
    }
    return o;
  }
}
