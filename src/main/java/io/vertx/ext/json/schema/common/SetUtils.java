package io.vertx.ext.json.schema.common;

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
