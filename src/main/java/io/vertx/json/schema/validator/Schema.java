package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.impl.BooleanSchema;
import io.vertx.json.schema.validator.impl.JsonSchema;

import java.util.Set;

@VertxGen
public interface Schema<T> {

  static Schema<JsonObject> fromJson(JsonObject json) {
    return new JsonSchema(json);
  }

  static Schema<Boolean> fromBoolean(boolean bool) {
    return new BooleanSchema(bool);
  }

  T unwrap();

  void annotate(String key, String value);

  <R> R get(String key);
  <R> R get(String key, R fallback);

  boolean contains(String key);

  Set<String> keys();
}
