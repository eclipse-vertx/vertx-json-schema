package io.vertx.ext.json.schema;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.VertxException;
import io.vertx.core.json.pointer.JsonPointer;

/**
 * This is the main class for every Validation flow related errors
 *
 * @author Francesco Guardiani @slinkydeveloper
 */
public class ValidationException extends VertxException {

  final private String keyword;
  final private Object input;
  private Schema schema;
  private JsonPointer scope;

  protected ValidationException(String message, String keyword, Object input) {
    super(message);
    this.keyword = keyword;
    this.input = input;
  }

  protected ValidationException(String message, Throwable cause, String keyword, Object input) {
    super(message, cause);
    this.keyword = keyword;
    this.input = input;
  }

  public static ValidationException createException(String message, String keyword, Object input, Throwable cause) {
    return new ValidationException(message, cause, keyword,  input);
  }

  public static ValidationException createException(String message, String keyword, Object input) {
    return new ValidationException(message, keyword, input);
  }

  /**
   * Returns the keyword that failed the validation, if any
   *
   * @return
   */
  @Nullable public String keyword() {
    return keyword;
  }

  /**
   * Returns the input that triggered the error
   *
   * @return
   */
  public Object input() {
    return input;
  }

  /**
   * Returns the schema that failed the validation
   *
   * @return
   */
  public Schema schema() {
    return schema;
  }

  /**
   * Returns the scope of the schema that failed the validation
   *
   * @return
   */
  public JsonPointer scope() {
    return scope;
  }

  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  public void setScope(JsonPointer scope) {
    this.scope = scope;
  }

  @Override
  public String toString() {
    return "ValidationException{" +
        "message='" + getMessage() + '\'' +
        ", keyword='" + keyword + '\'' +
        ", input=" + input +
        ", schema=" + schema +
        ((scope != null) ? ", scope=" + scope.toURI() : "") +
        '}';
  }
}
