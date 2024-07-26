package io.vertx.json.schema.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.internal.JsonObjectProxy;

public class JsonObjectRef extends JsonObjectProxy {

  public JsonObjectRef(JsonObject to) {
    super(to);
  }
}
