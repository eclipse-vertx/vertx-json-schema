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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SetUtils {

  /**
   * Returns the difference between initial and the operands
   */
  public static <T> Set<T> minus(Set<T> initial, Set<T> subtracted) {
    initial = new HashSet<>(initial);
    initial.removeAll(subtracted);
    return Collections.unmodifiableSet(initial);
  }

  public static <T> Set<T> minus(Set<T> initial, T subtracted) {
    initial = new HashSet<>(initial);
    initial.remove(subtracted);
    return Collections.unmodifiableSet(initial);
  }

  public static <T> Set<T> plus(Set<T> initial, Set<T> addend) {
    initial = new HashSet<>(initial);
    initial.addAll(addend);
    return Collections.unmodifiableSet(initial);
  }

  public static <T> Set<T> plus(Set<T> initial, T addend) {
    initial = new HashSet<>(initial);
    initial.add(addend);
    return Collections.unmodifiableSet(initial);
  }

  public static Set<Integer> range(int startInclusive, int endExclusive) {
    return IntStream
      .range(startInclusive, endExclusive)
      .boxed()
      .collect(Collectors.toSet());
  }

}
