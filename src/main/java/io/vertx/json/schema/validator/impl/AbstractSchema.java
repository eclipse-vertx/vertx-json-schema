package io.vertx.json.schema.validator.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.Schema;

public abstract class AbstractSchema {

  @SuppressWarnings("unchecked")
  public static <R> R wrap(JsonObject object, String key) {
    Object value = object.getValue(key);

    if (value instanceof Schema) {
      return (R) value;
    }

    if (value instanceof Boolean) {
      Schema schema = wrap((Boolean) value);
      return (R) schema;
    }

    if (value instanceof JsonObject) {
      Schema schema = wrap((JsonObject) value);
      object.put(key, schema);
      return (R) schema;
    }

    return (R) value;
  }

  @SuppressWarnings("unchecked")
  public static <R> R wrap(JsonArray array, int index) {
    Object value = array.getValue(index);

    if (value instanceof Schema) {
      return (R) value;
    }

    if (value instanceof Boolean) {
      Schema schema = wrap((Boolean) value);
      return (R) schema;
    }

    if (value instanceof JsonObject) {
      Schema schema = wrap((JsonObject) value);
      array.set(index, schema);
      return (R) schema;
    }

    return (R) value;
  }

  public static Schema wrap(Boolean value) {
    return value ?
      BooleanSchema.TRUE :
      BooleanSchema.FALSE;
  }

  public static Schema wrap(JsonObject value) {
    return new JsonSchema(value);
  }
}
