package io.vertx.json.schema.validator.impl;

import io.vertx.json.schema.validator.Schema;

import java.util.Set;

public class BooleanSchema implements Schema<Boolean> {

  private final boolean bool;

  public BooleanSchema(boolean bool) {
    this.bool = bool;
  }

  @Override
  public Boolean unwrap() {
    return bool;
  }

  @Override
  public void annotate(String key, String value) {
    throw new UnsupportedOperationException("This schema doesn't support annotate(String, String)");
  }

  @Override
  public <R> R get(String key) {
    throw new UnsupportedOperationException("This schema doesn't support get(String)");
  }

  @Override
  public <R> R get(String key, R fallback) {
    throw new UnsupportedOperationException("This schema doesn't support get(String, String)");
  }

  @Override
  public boolean contains(String key) {
    throw new UnsupportedOperationException("This schema doesn't support contains(String)");
  }

  @Override
  public Set<String> keys() {
    throw new UnsupportedOperationException("This schema doesn't support keys()");
  }

  @Override
  public String toString() {
    return Boolean.toString(bool);
  }
}
