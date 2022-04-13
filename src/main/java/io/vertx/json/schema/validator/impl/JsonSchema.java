package io.vertx.json.schema.validator.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.validator.Schema;

import java.util.Objects;

public class JsonSchema extends AbstractSchema<JsonObject> {

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
  public String toString() {
    return json.encodePrettily();
  }
}
