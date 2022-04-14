package io.vertx.json.schema.validator.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.Schema;

import java.util.Set;

public abstract class AbstractSchema<T> implements Schema<T> {

  private String __absolute_uri__;
  private String __absolute_ref__;
  private String __absolute_recursive_ref__;

  @Override
  public void annotate(String key, String value) {
    switch (key) {
      case "__absolute_uri__":
        __absolute_uri__ = value;
        break;
      case "__absolute_ref__":
        __absolute_ref__ = value;
        break;
      case "__absolute_recursive_ref__":
        __absolute_recursive_ref__ = value;
        break;
      default:
        throw new IllegalArgumentException("Unsupported annotation: " + key);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R get(String key, R fallback) {
    switch (key) {
      case "__absolute_uri__":
        return __absolute_uri__ != null ? (R) __absolute_uri__ : fallback;
      case "__absolute_ref__":
        return __absolute_ref__ != null ? (R) __absolute_ref__ : fallback;
      case "__absolute_recursive_ref__":
        return __absolute_recursive_ref__ != null ? (R) __absolute_recursive_ref__ : fallback;
      default:
        Object holder = unwrap();
        if (holder instanceof JsonObject) {
          return (R) ((JsonObject) holder).getValue(key, fallback);
        }
        throw new UnsupportedOperationException("This schema doesn't support get(String, String)");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <R> R get(String key) {
    switch (key) {
      case "__absolute_uri__":
        return (R) __absolute_uri__;
      case "__absolute_ref__":
        return (R) __absolute_ref__;
      case "__absolute_recursive_ref__":
        return (R) __absolute_recursive_ref__;
      default:
        Object holder = unwrap();
        if (holder instanceof JsonObject) {
          return (R) ((JsonObject) holder).getValue(key);
        }
        throw new UnsupportedOperationException("This schema doesn't support get(String)");
    }
  }

  @Override
  public boolean contains(String key) {
    switch (key) {
      case "__absolute_uri__":
        return __absolute_uri__ != null;
      case "__absolute_ref__":
        return __absolute_ref__ != null;
      case "__absolute_recursive_ref__":
        return __absolute_recursive_ref__ != null;
      default:
        Object holder = unwrap();
        if (holder instanceof JsonObject) {
          return ((JsonObject) holder).containsKey(key);
        }
        throw new UnsupportedOperationException("This schema doesn't support contains(String)");
    }
  }

  @Override
  public Set<String> keys() {
    Object holder = unwrap();
    if (holder instanceof JsonObject) {
      return ((JsonObject) holder).fieldNames();
    }
    throw new UnsupportedOperationException("This schema doesn't support keys()");
  }

  public static Schema<?> from(Object value) {
    if (value instanceof Boolean) {
      return Schema.fromBoolean((Boolean) value);
    }
    if (value instanceof JsonObject) {
      return Schema.fromJson((JsonObject) value);
    }
    return null;
  }
}
