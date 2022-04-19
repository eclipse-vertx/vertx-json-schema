package io.vertx.json.schema.validator.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.Schema;

public abstract class AbstractSchema {

  private static final Schema<Boolean> TRUE_SCHEMA = new BooleanSchema(true);
  private static final Schema<Boolean> FALSE_SCHEMA = new BooleanSchema(false);

  @SuppressWarnings("unchecked")
  public static <R> R wrap(JsonObject object, String key) {
    Object value = object.getValue(key);

    if (value instanceof Schema) {
      return (R) value;
    }

    if (value instanceof Boolean) {
      Schema<?> schema = wrap((Boolean) value);
      object.put(key, schema);
      return (R) schema;
    }

    if (value instanceof JsonObject) {
      Schema<?> schema = wrap((JsonObject) value);
      object.put(key, schema);
      return (R) schema;
    }

    return (R) value;
  }

  public static <R> R wrap(Schema<?> schema, String key) {
    return wrap(((JsonSchema) schema).unwrap(), key);
  }

  @SuppressWarnings("unchecked")
  public static <R> R wrap(JsonArray array, int index) {
    Object value = array.getValue(index);

    if (value instanceof Schema) {
      return (R) value;
    }

    if (value instanceof Boolean) {
      Schema<?> schema = wrap((Boolean) value);
      array.set(index, schema);
      return (R) schema;
    }

    if (value instanceof JsonObject) {
      Schema<?> schema = wrap((JsonObject) value);
      array.set(index, schema);
      return (R) schema;
    }

    return (R) value;
  }

  public static Schema<Boolean> wrap(Boolean value) {
    return value ?
      TRUE_SCHEMA :
      FALSE_SCHEMA;
  }

  public static Schema<JsonObject> wrap(JsonObject value) {
    return new JsonSchema(value);
  }
}
