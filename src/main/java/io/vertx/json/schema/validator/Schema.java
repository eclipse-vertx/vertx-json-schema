package io.vertx.json.schema.validator;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.impl.BooleanSchema;
import io.vertx.json.schema.validator.impl.JsonSchema;

import java.util.Set;

/**
 * @TODO: represents a schema, schemas can be either json or boolean, exposes a few method of jsonobject
 */
@VertxGen
public interface Schema {

  static Schema of(JsonObject json) {
    return new JsonSchema(json);
  }

  static Schema of(boolean bool) {
    return bool ?
      BooleanSchema.TRUE :
      BooleanSchema.FALSE;
  }

  void annotate(String key, String value);

  <R> R get(String key);
  <R> R get(String key, R fallback);

  boolean containsKey(String key);

  Set<String> fieldNames();
}
