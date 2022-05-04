package io.vertx.json.schema.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;

import java.util.HashSet;
import java.util.Set;

public final class JsonObjectSchema extends JsonObject implements JsonSchema {

  private boolean annotated;

  public JsonObjectSchema(JsonObject json) {
    super(json.getMap());
  }

  @Override
  public JsonSchema annotate(String key, String value) {
    switch (key) {
      case "__absolute_uri__":
        annotated = true;
        put("__absolute_uri__", value);
        break;
      case "__absolute_ref__":
        annotated = true;
        put("__absolute_ref__", value);
        break;
      case "__absolute_recursive_ref__":
        annotated = true;
        put("__absolute_recursive_ref__", value);
        break;
      default:
        throw new IllegalArgumentException("Unsupported annotation: " + key);
    }
    return this;
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

  @Override
  public Set<String> fieldNames() {
    if (annotated) {
      // filter out the annotations
      Set<String> filteredFieldNames = new HashSet<>(super.fieldNames());
      filteredFieldNames.remove("__absolute_uri__");
      filteredFieldNames.remove("__absolute_ref__");
      filteredFieldNames.remove("__absolute_recursive_ref__");
      return filteredFieldNames;
    } else {
      return super.fieldNames();
    }
  }
}
