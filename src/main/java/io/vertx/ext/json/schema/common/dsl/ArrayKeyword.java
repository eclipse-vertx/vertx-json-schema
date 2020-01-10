package io.vertx.ext.json.schema.common.dsl;

import java.util.function.Supplier;

public final class ArrayKeyword extends Keyword {
  public ArrayKeyword(String keyword, Object value) {
    super(keyword, value);
  }

  public ArrayKeyword(String keyword, Supplier<Object> value) {
    super(keyword, value);
  }
}
