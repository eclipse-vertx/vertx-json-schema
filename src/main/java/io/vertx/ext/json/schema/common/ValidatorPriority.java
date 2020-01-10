package io.vertx.ext.json.schema.common;

import io.vertx.codegen.annotations.VertxGen;

import java.util.Comparator;

@VertxGen
public enum ValidatorPriority {
  MAX_PRIORITY(0),
  MIN_PRIORITY(Integer.MAX_VALUE);

  Integer priority;

  ValidatorPriority(Integer value) {
    this.priority = value;
  }

  public Integer getPriority() {
    return priority;
  }

  public static Comparator<Validator> VALIDATOR_COMPARATOR = (v1, v2) -> {
    int res = v1.getPriority().getPriority().compareTo(v2.getPriority().getPriority());
    if (res == 0) return (v1.equals(v2)) ? 0 : +1; // Comparator need to be consistent with equals generic
    else return res;
  };

}
