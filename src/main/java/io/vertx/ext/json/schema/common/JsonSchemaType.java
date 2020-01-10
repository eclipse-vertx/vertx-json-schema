package io.vertx.ext.json.schema.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Objects;
import java.util.function.Predicate;

public enum JsonSchemaType {
  NULL(Objects::isNull),
  BOOLEAN(o -> o instanceof Boolean),
  OBJECT(o -> o instanceof JsonObject),
  ARRAY(o -> o instanceof JsonArray),
  NUMBER(o -> o instanceof Number),
  NUMBER_DECIMAL(o -> o instanceof Double || o instanceof Float),
  INTEGER(o -> o instanceof Long || o instanceof Integer),
  STRING(o -> o instanceof String);

  private final Predicate<Object> checkInstancePredicate;

  JsonSchemaType(Predicate<Object> checkInstancePredicate) {
    this.checkInstancePredicate = checkInstancePredicate;
  }

  public boolean checkInstance(Object obj) {
    return checkInstancePredicate.test(obj);
  }
}
