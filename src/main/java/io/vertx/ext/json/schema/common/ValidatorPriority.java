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
