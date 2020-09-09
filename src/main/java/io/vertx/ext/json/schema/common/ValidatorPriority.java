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

import io.vertx.codegen.annotations.VertxGen;

import java.util.Comparator;

@VertxGen
public enum ValidatorPriority {
  MAX_PRIORITY(0),
  NORMAL_PRIORITY(2),
  CONTEXTUAL_VALIDATOR(Integer.MAX_VALUE);

  private final Integer priority;

  ValidatorPriority(Integer value) {
    this.priority = value;
  }

  public static Comparator<PriorityGetter> COMPARATOR = (v1, v2) -> {
    int res = v1.getPriority().priority.compareTo(v2.getPriority().priority);
    if (res == 0) return (v1.equals(v2)) ? 0 : +1; // Comparator need to be consistent with equals generic
    else return res;
  };

}
