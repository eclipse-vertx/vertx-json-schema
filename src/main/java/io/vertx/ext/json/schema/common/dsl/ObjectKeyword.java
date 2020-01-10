package io.vertx.ext.json.schema.common.dsl;

import java.util.function.Supplier;

public final class ObjectKeyword extends Keyword {
  public ObjectKeyword(String keyword, Object value) {
    super(keyword, value);
  }

  public ObjectKeyword(String keyword, Supplier<Object> value) {
    super(keyword, value);
  }
}
