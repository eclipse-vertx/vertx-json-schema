package io.vertx.ext.json.schema;

import io.vertx.core.VertxException;
import io.vertx.ext.json.schema.common.MutableStateValidator;

/**
 * This exception is thrown when you call {@link Schema#validateSync(Object)} when the schema is in an asynchronous state
 *
 */
public class NoSyncValidationException extends VertxException {

  private MutableStateValidator validator;

  public NoSyncValidationException(String message, MutableStateValidator validator) {
    super(message);
    this.validator = validator;
  }

  public NoSyncValidationException(String message, Throwable cause, MutableStateValidator validator) {
    super(message, cause);
    this.validator = validator;
  }

  public MutableStateValidator getValidator() {
    return validator;
  }
}
