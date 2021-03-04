package io.vertx.json.schema.common;

import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.ValidationException;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class ValidationExceptionImpl extends ValidationException {

  public ValidationExceptionImpl(String message, String keyword, Object input) {
    super(message, keyword, input);
  }

  public ValidationExceptionImpl(String message, Throwable cause, String keyword, Object input) {
    super(message, cause, keyword, input);
  }

  public ValidationExceptionImpl(String message, Collection<Throwable> causes, String keyword, Object input) {
    super(message + ". Multiple causes: " + formatExceptions(causes), keyword, input);
  }

  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  public void setInputScope(JsonPointer scope) {
    this.inputScope = scope;
  }

  private static String formatExceptions(Collection<Throwable> throwables) {
    if (throwables == null) {
      return "[]";
    }
    return "[" + throwables
      .stream()
      .filter(Objects::nonNull)
      .map(Throwable::getMessage)
      .collect(Collectors.joining(", ")) + "]";
  }
}
