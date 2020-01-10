package io.vertx.ext.json.schema;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.VertxException;

/**
 * This class represents an error while parsing a {@link Schema}
 *
 * @author slinkydeveloper
 */
public class SchemaException extends VertxException {

  private Object schema;

  public SchemaException(Object schema, String message, Throwable cause) {
    super(message, cause);
    this.schema = schema;
  }

  public SchemaException(Object schema, String message) {
    super(message);
    this.schema = schema;
  }

  /**
   * Json representation of the schema
   *
   * @return
   */
  public Object schema() {
    return schema;
  }

  @Override
  public String toString() {
    return "SchemaException{" +
        "message=\'" + getMessage() + "\'" +
        ", schema=" + schema +
        '}';
  }
}
