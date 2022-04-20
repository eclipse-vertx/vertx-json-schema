package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.impl.AbstractSchema;

import java.util.Set;

@VertxGen
public interface Schema {

  static Schema fromJson(JsonObject json) {
    return AbstractSchema.wrap(json);
  }

  static Schema fromBoolean(boolean bool) {
    return AbstractSchema.wrap(bool);
  }

  void annotate(String key, String value);

  <R> R get(String key);
  <R> R get(String key, R fallback);

  boolean containsKey(String key);

  Set<String> fieldNames();
}
