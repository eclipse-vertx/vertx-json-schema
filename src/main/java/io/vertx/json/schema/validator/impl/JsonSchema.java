package io.vertx.json.schema.validator.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.Schema;

import java.util.*;
import java.util.function.Consumer;

public class JsonSchema implements Schema<JsonObject> {

  private static final List<String> NON_ENUMERABLE = Arrays.asList(
    "__absolute_uri__",
    "__absolute_ref__",
    "__absolute_recursive_ref__");

  private final JsonObject json;

  public JsonSchema(JsonObject json) {
    this.json = json;
  }

  public JsonSchema(Schema<JsonObject> schema, String id) {
    Objects.requireNonNull(id);
    this.json = schema.unwrap()
      // make a shallow copy to avoid mutating the original schema
      .copy()
      // add a id
      .put("$id", id);
  }

  @Override
  public JsonObject unwrap() {
    return json;
  }

  @Override
  public void annotate(String key, String value) {
    switch (key) {
      case "__absolute_uri__":
        json.put("__absolute_uri__", value);
        break;
      case "__absolute_ref__":
        json.put("__absolute_ref__", value);
        break;
      case "__absolute_recursive_ref__":
        json.put("__absolute_recursive_ref__", value);
        break;
      default:
        throw new IllegalArgumentException("Unsupported annotation: " + key);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R get(String key, R fallback) {
    return (R) json.getValue(key, fallback);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R get(String key) {
    return (R) json.getValue(key);
  }

  @Override
  public boolean contains(String key) {
    return json.containsKey(key);
  }

  @Override
  public Set<String> keys() {
    Set<String> allKeys = new HashSet<>(json.fieldNames());
    // remove the non-enumerable
    NON_ENUMERABLE.forEach(allKeys::remove);
    return allKeys;
  }

  @Override
  public String toString() {
    return json.encode();
  }
}
