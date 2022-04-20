package io.vertx.json.schema.validator.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.Schema;

public class JsonSchema extends JsonObject implements Schema {

  public JsonSchema(JsonObject json) {
    super(json.getMap());
  }

  @Override
  public void annotate(String key, String value) {
    switch (key) {
      case "__absolute_uri__":
        put("__absolute_uri__", value);
        break;
      case "__absolute_ref__":
        put("__absolute_ref__", value);
        break;
      case "__absolute_recursive_ref__":
        put("__absolute_recursive_ref__", value);
        break;
      default:
        throw new IllegalArgumentException("Unsupported annotation: " + key);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R get(String key, R fallback) {
    return (R) getValue(key, fallback);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R get(String key) {
    return (R) getValue(key);
  }
}
