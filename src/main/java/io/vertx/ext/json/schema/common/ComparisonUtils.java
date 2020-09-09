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

import java.util.Objects;

public class ComparisonUtils {

  /*
   In Json Schema there is no difference between different number sizing.
   So we eventually need to coerce types
   */
  public static boolean equalsNumberSafe(Object a, Object b) {
    if (a == null || b == null) {
      return a == null && b == null;
    }

    if (a.getClass().equals(b.getClass())) {
      return Objects.equals(a, b);
    }

    if (a instanceof Number && b instanceof Number) {
      if ((a instanceof Integer && b instanceof Long) || (a instanceof Long && b instanceof Integer)) {
        return ((Number) a).longValue() == ((Number) b).longValue();
      }
      return ((Number) a).doubleValue() == ((Number) b).doubleValue();

    }
    return Objects.equals(a, b);
  }

}
