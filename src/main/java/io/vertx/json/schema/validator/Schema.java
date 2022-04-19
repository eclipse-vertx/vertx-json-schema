package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.impl.AbstractSchema;

import java.util.Set;

@VertxGen
public interface Schema<T> {

  static Schema<JsonObject> fromJson(JsonObject json) {
    return AbstractSchema.wrap(json);
  }

  static Schema<Boolean> fromBoolean(boolean bool) {
    return AbstractSchema.wrap(bool);
  }

  T unwrap();

  void annotate(String key, String value);

  <R> R get(String key);
  <R> R get(String key, R fallback);

  boolean contains(String key);

  Set<String> keys();
}
