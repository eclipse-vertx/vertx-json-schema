package io.vertx.json.schema.validator.impl;

public class BooleanSchema extends AbstractSchema<Boolean> {

  private final boolean bool;

  public BooleanSchema(boolean bool) {
    this.bool = bool;
  }

  @Override
  public Boolean unwrap() {
    return bool;
  }

  @Override
  public String toString() {
    return Boolean.toString(bool);
  }
}
