package io.vertx.ext.json.schema.common;

public interface Validator extends PriorityGetter {

  /**
   * Returns true if this validator can actually provide a synchronous validation
   *
   * @return
   */
  boolean isSync();

}
